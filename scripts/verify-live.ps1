param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
function Read-CsrfToken([string]$Html) {
    $tokenMatch = [regex]::Match($Html, '<input\b(?=[^>]*\bname="_csrf")(?=[^>]*\bvalue="([^"]+)")[^>]*>')
    if (-not $tokenMatch.Success) { throw 'A server-rendered CSRF token was not found.' }
    [System.Net.WebUtility]::HtmlDecode($tokenMatch.Groups[1].Value)
}
$rentalClock = [System.Diagnostics.Stopwatch]::StartNew()
$rentalHome = Invoke-WebRequest -Uri "$BaseUrl/" -SessionVariable rentalSession -TimeoutSec 120
$rentalClock.Stop()
if ($rentalHome.StatusCode -ne 200 -or -not $rentalHome.Content.Contains('Loc Auto')) { throw 'The real Loc Auto homepage was not rendered.' }
if ($rentalHome.Content.Contains(';jsessionid=')) { throw 'Session IDs must not appear in URLs.' }
$guest = Invoke-WebRequest -Uri "$BaseUrl/demo" -Method Post -WebSession $rentalSession -Body @{
    _csrf = (Read-CsrfToken $rentalHome.Content)
} -TimeoutSec 120
if ($guest.BaseResponse.RequestMessage.RequestUri.AbsolutePath -ne '/voitures') { throw 'Isolated demo visitor access failed.' }
$form = Invoke-WebRequest -Uri "$BaseUrl/locations/new/AA-123-BB" -WebSession $rentalSession -TimeoutSec 120
$reservation = Invoke-WebRequest -Uri "$BaseUrl/locations/new/AA-123-BB" -Method Post -WebSession $rentalSession -Body @{
    _csrf = (Read-CsrfToken $form.Content)
    dteDeb = (Get-Date).AddDays(1).ToString('yyyy-MM-dd')
    duree = '2'
    cheque = 'VERCEL-DEMO-VERIFICATION'
} -TimeoutSec 120
$detailPath = $reservation.BaseResponse.RequestMessage.RequestUri.AbsolutePath
if ($detailPath -notmatch '^/locations/\d+$' -or -not $reservation.Content.Contains('AA-123-BB')) { throw 'A real fictional booking was not created.' }
try {
    $history = Invoke-WebRequest -Uri "$BaseUrl/locations" -WebSession $rentalSession -TimeoutSec 120
    if (-not $history.Content.Contains('AA-123-BB')) { throw 'Booking missing from server history.' }
} finally {
    $finished = Invoke-WebRequest -Uri "$BaseUrl$detailPath/terminer" -Method Post -WebSession $rentalSession -Body @{
        _csrf = (Read-CsrfToken $reservation.Content)
    } -TimeoutSec 120
}
if ($finished.BaseResponse.RequestMessage.RequestUri.AbsolutePath -ne '/locations') { throw 'Vehicle return failed.' }
$profile = Invoke-WebRequest -Uri "$BaseUrl/profile" -WebSession $rentalSession -TimeoutSec 120
$logout = Invoke-WebRequest -Uri "$BaseUrl/logout" -Method Post -WebSession $rentalSession -Body @{
    _csrf = (Read-CsrfToken $profile.Content)
} -TimeoutSec 120
if ($logout.BaseResponse.RequestMessage.RequestUri.AbsolutePath -ne '/login') { throw 'POST logout failed.' }
$privateAfterLogout = Invoke-WebRequest -Uri "$BaseUrl/locations" -WebSession $rentalSession -TimeoutSec 120
if ($privateAfterLogout.BaseResponse.RequestMessage.RequestUri.AbsolutePath -ne '/login') { throw 'A private route remained accessible after logout.' }
[pscustomobject]@{
    BaseUrl = $BaseUrl
    InitialPageSeconds = [Math]::Round($rentalClock.Elapsed.TotalSeconds, 2)
    Visitor = 'isolated fictional account'
    Csrf = 'real rendered tokens accepted'
    Booking = 'passed'
    History = 'passed'
    Return = 'passed'
    Logout = 'POST with CSRF passed; private access rejected afterward'
    Data = 'Fictional demonstration data only; vehicle released.'
}
