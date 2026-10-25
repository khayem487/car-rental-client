package com.carrental.client;

import com.carrental.client.dao.PersonneDAO;
import com.carrental.client.model.Location;
import com.carrental.client.model.Personne;
import com.carrental.client.security.OidcAccounts;
import com.carrental.client.security.RentalOidcUser;
import com.carrental.client.service.LocationService;
import com.carrental.client.service.PersonneService;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import com.carrental.client.dao.LocationDAO;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

import static org.assertj.core.api.Assertions.*;

/** Opt-in, real disposable PostgreSQL only; never falls back to H2 or a production URL. */
@EnabledIfEnvironmentVariable(named = "RENTAL_TEST_DATABASE_URL", matches = ".+")
class PostgresPersistenceTest {
    private final String url = System.getenv("RENTAL_TEST_DATABASE_URL");

    @Test void dataSessionsAndGoogleStateSurviveAnotherInstanceAndRestartAndBookingHasOneWinner() throws Exception {
        URI database = URI.create(url.replaceFirst("^jdbc:", ""));
        assertThat(database.getHost()).isIn("127.0.0.1", "localhost");
        assertThat(database.getPath()).startsWith("/rental_test");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String login = "persist." + suffix, car = "TEST-" + suffix;
        Browser browser = new Browser();
        Browser oauth = new Browser();
        Long userId, booking;
        String oauthSessionId, state;
        ConfigurableApplicationContext first = start(), second = null;
        try {
            JdbcTemplate jdbc = first.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT version()", String.class)).contains("PostgreSQL");
            Personne person = person(login, suffix);
            userId = first.getBean(PersonneService.class).enregistrer(person);
            assertThat(jdbc.queryForObject("SELECT passwd FROM personne WHERE id=?", String.class, userId)).startsWith("$2");
            jdbc.update("INSERT INTO voiture(mat,marque,model,nbplace,disp,prix) VALUES (?,?,?,5,'DISPONIBLE',42.50)", car, "Test", "Persistent");
            String csrf = csrf(browser.get(first, "/login").body());
            assertThat(browser.post(first, "/login", Map.of("_csrf", csrf, "login", login, "passwd", "testpassword")).statusCode()).isEqualTo(302);
            assertThat(browser.get(first, "/profile").statusCode()).isEqualTo(200);
            assertThat(browser.post(first, "/locations/new/" + car, Map.of("dteDeb", LocalDate.now().toString(), "duree", "2", "cheque", "NO-CSRF")).statusCode()).isEqualTo(403);
            csrf = csrf(browser.get(first, "/locations/new/" + car).body());
            var created = browser.post(first, "/locations/new/" + car, Map.of("_csrf", csrf, "dteDeb", LocalDate.now().toString(), "duree", "2", "cheque", "PERSIST-" + suffix));
            assertThat(created.statusCode()).isEqualTo(302);
            booking = Long.parseLong(created.headers().firstValue("location").orElseThrow().replaceFirst(".*/locations/", ""));

            var redirect = oauth.get(first, "/oauth2/authorization/google");
            assertThat(redirect.statusCode()).isEqualTo(302);
            assertThat(redirect.headers().firstValue("location").orElseThrow()).contains("state=", "nonce=");
            oauthSessionId = oauth.sessionId();
            var savedRequest = authorization(first, oauthSessionId);
            state = savedRequest.getState();
            assertThat(savedRequest.getAdditionalParameters()).containsKey("nonce");

            second = start();
            assertThat(browser.get(second, "/profile").body()).contains(login);
            String sharedCsrf = csrf(browser.get(first, "/profile").body());
            assertThat(browser.post(second, "/profile/update", Map.of("_csrf", sharedCsrf, "nom", "Fictif", "prenom", "Persistant"))
                .statusCode()).isEqualTo(302);
            assertThat(browser.get(second, "/locations/" + booking).body()).contains(car);
            assertThat(authorization(second, oauthSessionId).getState()).isEqualTo(state);
            assertThat(authorization(second, oauthSessionId).getAdditionalParameters().get("nonce"))
                .isEqualTo(savedRequest.getAdditionalParameters().get("nonce"));
            assertThat(second.getBean(PersonneDAO.class).findById(userId)).isPresent();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class)).isEqualTo(3);

            // A serializable Google principal is also read from PostgreSQL by a second application context.
            String subject = "pg-" + suffix;
            Instant now = Instant.now();
            var identity = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")),
                new OidcIdToken("fictional-id-token", now, now.plusSeconds(300), Map.of("iss", "https://accounts.google.com", "sub", subject,
                    "email", suffix + "@example.test", "email_verified", true)));
            Long googleId = first.getBean(OidcAccounts.class).resolve(identity);
            var repo = sessions(first);
            var googleSession = repo.findById(oauthSessionId);
            var principal = new RentalOidcUser(identity, googleId);
            googleSession.setAttribute("SPRING_SECURITY_CONTEXT", new SecurityContextImpl(new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google")));
            repo.save(googleSession);
            assertThat(oauth.get(second, "/profile").body()).contains("Connexion Google");
            assertThat(second.getBean(OidcAccounts.class).resolve(identity)).isEqualTo(googleId);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oidc_identity WHERE subject=?", Integer.class, subject)).isEqualTo(1);

            first.close(); second.close(); second = null;
            first = start();
            assertThat(browser.get(first, "/profile").body()).contains(login);
            assertThat(browser.get(first, "/locations/" + booking).body()).contains(car);
            assertThat(oauth.get(first, "/profile").body()).contains("Connexion Google");
            assertThat(authorization(first, oauthSessionId).getState()).isEqualTo(state);
            assertThat(first.getBean(JdbcTemplate.class).queryForObject("SELECT disp FROM voiture WHERE mat=?", String.class, car)).isEqualTo("LOUEE");
            assertThat(first.getBean(JdbcTemplate.class).queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success", Integer.class)).isEqualTo(3);

            LocationService service = first.getBean(LocationService.class);
            service.terminerLocation(booking);
            second = start();
            LocationService otherInstance = second.getBean(LocationService.class);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            CountDownLatch go = new CountDownLatch(1);
            final Long owner = userId;
            java.util.function.Function<LocationService, Callable<Long>> contender = target -> () -> {
                go.await(); try { return target.createLocation(location(owner, car, "RACE-" + suffix)); }
                catch (IllegalArgumentException lost) { return null; } };
            Future<Long> a = executor.submit(contender.apply(service)), b = executor.submit(contender.apply(otherInstance));
            go.countDown();
            try { assertThat(java.util.stream.Stream.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)).filter(java.util.Objects::nonNull).count()).isEqualTo(1); }
            finally { executor.shutdownNow(); }
            jdbc = first.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM location WHERE mat=? AND etat='EN_COURS'", Integer.class, car)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM location WHERE cheque=?", Integer.class, "RACE-" + suffix)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM personne WHERE login='jean.dupont'", Integer.class)).isEqualTo(1);
            var shared = sessions(first);
            Session expired = shared.createSession();
            expired.setLastAccessedTime(Instant.now().minusSeconds(3600));
            expired.setMaxInactiveInterval(java.time.Duration.ofSeconds(1));
            shared.save(expired);
            assertThat(shared.findById(expired.getId())).isNull();
            assertThatThrownBy(() -> service.terminerLocation(booking)).isInstanceOf(IllegalArgumentException.class);
            assertThat(jdbc.queryForObject("SELECT disp FROM voiture WHERE mat=?", String.class, car)).isEqualTo("LOUEE");
            // An insert failure after the atomic vehicle claim must restore availability.
            Long active = jdbc.queryForObject("SELECT id_location FROM location WHERE mat=? AND etat='EN_COURS'", Long.class, car);
            service.terminerLocation(active);
            Location invalid = location(owner, car, "X".repeat(51));
            var dao = first.getBean(LocationDAO.class);
            var transaction = new TransactionTemplate(first.getBean(PlatformTransactionManager.class));
            assertThatThrownBy(() -> transaction.execute(status -> dao.save(invalid))).isInstanceOf(RuntimeException.class);
            assertThat(jdbc.queryForObject("SELECT disp FROM voiture WHERE mat=?", String.class, car)).isEqualTo("DISPONIBLE");
            String token = csrf(browser.get(first, "/profile").body());
            assertThat(browser.post(first, "/logout", Map.of("_csrf", token)).statusCode()).isEqualTo(302);
            assertThat(browser.get(first, "/profile").statusCode()).isEqualTo(302);
        } finally {
            first.close();
            if (second != null) second.close();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static SessionRepository<Session> sessions(ConfigurableApplicationContext app) {
        return (SessionRepository) app.getBean(JdbcIndexedSessionRepository.class);
    }
    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(CarRentalClientApplication.class).run("--spring.profiles.active=persistent", "--server.port=0",
            "--DATABASE_URL=" + url, "--DATABASE_URL_UNPOOLED=" + url, "--app.database.allow-insecure-local=true",
            "--SESSION_COOKIE_SECURE=false", "--GOOGLE_CLIENT_ID=fictional-client", "--GOOGLE_CLIENT_SECRET=fictional-secret",
            "--logging.level.root=WARN", "--spring.main.banner-mode=off");
    }
    private static Personne person(String login, String suffix) {
        Personne person = new Personne(); person.setLogin(login); person.setNom("Fictif"); person.setPrenom("Persistant");
        person.setCin("TEST-" + suffix); person.setNumero("DEMO"); person.setNpermis("DEMO"); person.setPasswd("testpassword"); return person;
    }
    private static Location location(Long user, String car, String reference) {
        Location location = new Location(); location.setId_personne(user); location.setMat(car); location.setDteDeb(java.sql.Date.valueOf(LocalDate.now()));
        location.setDuree(2); location.setCheque(reference); return location;
    }
    private static OAuth2AuthorizationRequest authorization(ConfigurableApplicationContext app, String session) {
        Session saved = sessions(app).findById(session);
        assertThat(saved).isNotNull();
        return saved.getAttribute("org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST");
    }
    private static String csrf(String html) {
        var match = Pattern.compile("<input\\b(?=[^>]*\\bname=\"_csrf\")(?=[^>]*\\bvalue=\"([^\"]+)\")[^>]*>").matcher(html);
        assertThat(match.find()).isTrue(); return match.group(1);
    }
    private static class Browser {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
        private URI uri(ConfigurableApplicationContext app, String path) { return URI.create("http://127.0.0.1:" + app.getEnvironment().getRequiredProperty("local.server.port") + path); }
        HttpResponse<String> get(ConfigurableApplicationContext app, String path) throws Exception { return client.send(HttpRequest.newBuilder(uri(app, path)).GET().build(), HttpResponse.BodyHandlers.ofString()); }
        HttpResponse<String> post(ConfigurableApplicationContext app, String path, Map<String, String> params) throws Exception {
            String body = params.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(java.util.stream.Collectors.joining("&"));
            return client.send(HttpRequest.newBuilder(uri(app, path)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        }
        String sessionId() { return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue(); }
    }
}
