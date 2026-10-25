# Durable PostgreSQL demo

The `persistent` profile stores fictional accounts, Google subject mappings, bookings and HTTP sessions in PostgreSQL. The default Docker/Vercel configuration remains the resetting H2 `demo` profile until a database is explicitly connected. Both profiles keep the educational demo UI: fictional identity references, isolated visitor accounts, no payment.

## Vercel / Neon configuration

Create or connect a dedicated PostgreSQL database through the provider's own account/consent flow, then configure server-only environment variables in the intended deployment environment:

| Variable | Value |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `persistent` (alone; never `demo,persistent`) |
| `DATABASE_URL` | Provider's pooled `postgresql://user:password@host/database?sslmode=require` URL |
| `DATABASE_URL_UNPOOLED` | Optional direct provider URL, used only for migrations; recommended with Neon |
| `SESSION_COOKIE_SECURE` | `true` for HTTPS deployments |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Optional; both are required to enable Google login |

Keep preview/test databases separate from production. Do not put credentials into this repository, browser JavaScript, CLI output, screenshots or chat. See [Google setup](GOOGLE-LOGIN.md) for consent/callback requirements; adding storage does not itself enable Google.

The URL converter accepts `postgres://`, `postgresql://` and `jdbc:postgresql://`, including percent-encoded credentials. Credentials are removed from the JDBC URL and supplied separately. Accepted URL parameters are `sslmode`, `channel_binding`/`channelBinding`, `connectTimeout`, `socketTimeout`, and `ApplicationName`. Other options fail closed with a generic error; no input URI is echoed. TLS defaults to `verify-full` and accepts `require`, `verify-ca`, or `verify-full`. `sslmode=disable` is allowed only on `localhost`/loopback when the explicit local test property `app.database.allow-insecure-local=true` is set. Never set that property on a hosted deployment.

The web pool uses at most three connections, starts with zero idle connections, retires idle connections after one minute, and sends no keep-alive. Migrations use the direct URL if provided and run before the web pool is available. No sequence session state is required by PostgreSQL inserts, so transaction pooling is supported.

## Migrations and retention

Flyway applies versioned migrations under `db/migration/postgresql` once and checks their checksums on later starts:

- V1 creates rental/account/Google identity tables with foreign keys and a unique active booking per car.
- V2 inserts only missing fictional demo users and catalogue vehicles. It does not overwrite later profile/password/availability changes. Generated IDs are allocated by the database.
- V3 creates shared HTTP session tables.

There are no `DROP`/reset statements. SQL script auto-initialization is disabled. Flyway clean is disabled, and automatic baselining is disabled: an existing unversioned schema is refused for explicit review rather than silently adopted. Add a new migration for future changes; never edit an already applied file. Back up real durable data before future schema changes.

This starts a **new** durable database; it does not copy a running instance's old H2 in-memory data or browser sessions. The original Oracle configuration/manual scripts remain available and H2 demo remains separate. Oracle and H2 insert IDs come from their original sequence on the same JDBC connection; PostgreSQL uses generated keys. Oracle was preserved in source but was not available for a live database regression.

Shared sessions retain local/Google authenticated principals, CSRF tokens, flash attributes and the pending Google authorization request/state/nonce across application instances and restarts. Cookies retain the `JSESSIONID` name, HttpOnly and SameSite=Lax; HTTPS deployments must set Secure. Sessions expire after 30 minutes of inactivity. Logout invalidates the shared record. Java-serialized session attributes assume compatible application/security library versions: a future incompatible upgrade may require users to sign in again.

The recurring Spring Session cleanup job is disabled with `cleanupCron="-"`; this is explicitly supported in [Spring Session 4.1.1 source](https://github.com/spring-projects/spring-session/blob/4.1.1/spring-session-jdbc/src/main/java/org/springframework/session/jdbc/JdbcIndexedSessionRepository.java). Expired sessions are rejected/deleted when read, and one bulk cleanup runs at application start. No scheduled traffic prevents Neon from sleeping. Unvisited expired rows can remain until the next cold start; if a long-lived deployment needs additional cleanup, run an occasional maintenance deletion of expired `SPRING_SESSION` rows (attributes cascade), rather than a keep-alive loop.

## Verification

Normal offline suite: `./mvnw test` from `car-rental-client/` (Windows `./mvnw.cmd test`). The PostgreSQL integration test is explicitly opt-in and never substitutes H2:

```powershell
# Use a dedicated disposable local PostgreSQL database named rental_test or rental_test_*.
$env:RENTAL_TEST_DATABASE_URL = 'postgresql://TEST_USER:TEST_PASSWORD@127.0.0.1:55432/rental_test?sslmode=disable'
./mvnw.cmd test
Remove-Item Env:RENTAL_TEST_DATABASE_URL
```

The test accepts only loopback and a database name beginning `rental_test`, adds unique fictional rows, starts two full HTTP application contexts on one database, then closes/restarts them. It checks account/password/booking retention, shared authenticated sessions, stored Google state/nonce, a serialized Google principal, same-subject identity stability, CSRF rejection, logout, expiry, a concurrent booking with one winner, return protection and a database insert failure rolling back its preceding vehicle claim. Repeated migrations leave the seed rows and previous data intact. It does not exercise Google's real external token/consent service or a Vercel redeployment.

Before changing a public deployment, run the real browser login/booking/history/return flow there, redeploy, and confirm the same fictional data/session survives. A local test proves the application/PostgreSQL behavior, not provider account configuration or a completed production rollout.
