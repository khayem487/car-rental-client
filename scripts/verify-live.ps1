param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$rentalClock = [System.Diagnostics.Stopwatch]::StartNew()
$loginPage = Invoke-WebRequest -Uri "$BaseUrl/login" -SessionVariable rentalSession -TimeoutSec 120
$rentalClock.Stop()
if ($loginPage.StatusCode -ne 200 -or -not $loginPage.Content.Contains('Loc Auto')) {
    throw 'The public login endpoint did not return the real Loc Auto application.'
}
$login = Invoke-WebRequest -Uri "$BaseUrl/login" -Method Post -WebSession $rentalSession -Body @{
    login = 'sophie.martin'; passwd = 'password123'
} -TimeoutSec 120
if ($login.BaseResponse.RequestMessage.RequestUri.AbsolutePath -ne '/') { throw 'Demo login failed.' }
$catalog = Invoke-WebRequest -Uri "$BaseUrl/voitures" -WebSession $rentalSession -TimeoutSec 120
if (-not $catalog.Content.Contains('Renault')) { throw 'The real catalog was not rendered.' }
$reservation = Invoke-WebRequest -Uri "$BaseUrl/locations/new/AA-123-BB" -Method Post -WebSession $rentalSession -Body @{
    dteDeb = (Get-Date).AddDays(1).ToString('yyyy-MM-dd'); duree = '2'; cheque = 'VERCEL-DEMO-VERIFICATION'
} -TimeoutSec 120
$detailPath = $reservation.BaseResponse.RequestMessage.RequestUri.AbsolutePath
if ($detailPath -notmatch '^/locations/\d+$' -or -not $reservation.Content.Contains('AA-123-BB')) {
    throw 'The real server reservation was not created.'
}
try {
    $history = Invoke-WebRequest -Uri "$BaseUrl/locations" -WebSession $rentalSession -TimeoutSec 120
    if (-not $history.Content.Contains('AA-123-BB')) { throw 'Reservation missing from server history.' }
} finally {
    # Release the fictional demonstration vehicle, even when verification fails.
    $finished = Invoke-WebRequest -Uri "$BaseUrl$detailPath/terminer" -Method Post -WebSession $rentalSession -TimeoutSec 120
}
if ($finished.BaseResponse.RequestMessage.RequestUri.AbsolutePath -ne '/locations') { throw 'Return failed.' }
Invoke-WebRequest -Uri "$BaseUrl/logout" -WebSession $rentalSession -TimeoutSec 120 | Out-Null
[pscustomobject]@{
    BaseUrl = $BaseUrl
    InitialLoginSeconds = [Math]::Round($rentalClock.Elapsed.TotalSeconds, 2)
    Login = 'passed'; Catalog = 'passed'; Reservation = 'passed'; History = 'passed'; Return = 'passed'; Logout = 'passed'
    Data = 'Existing fictional H2 demo records only; test vehicle released.'
}
