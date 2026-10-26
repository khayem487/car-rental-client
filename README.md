# Car Rental Client Application

A web-based car rental system for clients to browse available cars, make reservations, and manage their rental history.

## Technologies Used

- Backend: Java 17 + Spring Boot 4.1 + Spring Security + JDBC
- Frontend: Thymeleaf + CSS + JavaScript
- Databases:
  - Oracle DB (original stack)
  - H2 in-memory (local `demo` profile and Render fallback)
  - PostgreSQL (explicit `persistent` profile with Flyway migrations and shared JDBC sessions)
- Build Tool: Maven

## Features

- User authentication (login/registration, BCrypt passwords and CSRF protection)
- Google OpenID Connect login on the public demo; optional server configuration for other deployments
- Browse available cars with filtering options
- Car rental reservation system
- User profile management
- Rental history tracking
- Responsive design

## Render fallback (H2)

This repository includes a `render.yaml` + `Dockerfile` and an H2-backed `demo` Spring profile.

- Deploy button (free tier):

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/khayem487/car-rental-client)

> Render free web services may sleep after inactivity.

## Run Locally

### Option A — Original Oracle mode

1. Configure Oracle DB with:
   - username: `c##loc`
   - password: `loc`
2. Initialize schema/data from `car-rental-client/src/main/resources/schema_and_data.sql`
3. Run:

```bash
cd car-rental-client
./mvnw clean package
./mvnw spring-boot:run
```

### Option B — Demo mode (H2, no Oracle required)

```bash
cd car-rental-client
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Then open: `http://localhost:8080`

## Project Structure

This application is the client-facing part of a car rental system. It supports the original Oracle setup, H2 for a local demo, and PostgreSQL for durable hosted data and shared sessions.

## License

[MIT License](LICENSE)

<!-- trigger linguist rescan -->

## Real application on Vercel

The same Spring Boot / JDBC / Thymeleaf application is available at:

[Open Loc Auto](https://khayem-car-rental.vercel.app/)

The public deployment uses the `persistent` Spring profile with **Neon Free PostgreSQL in Frankfurt**. Accounts, Google identity mappings, catalogue data, reservations and HTTP sessions are stored in PostgreSQL. Flyway applies versioned migrations without resetting existing data. The forwarded-header setting preserves HTTPS redirects behind the Vercel proxy.

`Dockerfile.vercel` builds the Java application with Maven and defaults to the H2 `demo` profile. Production overrides that default with `SPRING_PROFILES_ACTIVE=persistent` and its server-side database configuration. Follow [the PostgreSQL setup guide](docs/POSTGRESQL.md) for another deployment; never combine the resetting `demo` profile with `persistent`.

```sh
npx vercel@64.7.0 deploy --prod
```

Reservations remain fictional demonstration data, and no payment is collected. Containers may cold-start, while the database retains the accounts, reservations and unexpired sessions across application restarts and redeployments. This is not a promise of a continuously running JVM or uninterrupted availability.

Public retention verification on **25 October 2026**: an isolated fictional visitor created a booking, production was redeployed, and the same browser session still opened its profile, history and the same owned booking on the new deployment. The test then returned the vehicle and logged out. Google sign-in, callback, logout and sign-in again were verified in the real browser flow on **26 October 2026**, retaining the same application account.

```powershell
./scripts/verify-live.ps1 -BaseUrl https://khayem-car-rental.vercel.app
```

Render configuration is retained as a fallback. The Vercel project is connected to this GitHub repository: a push to `master` triggers a production build. Direct authenticated Vercel CLI publication also works.

## Loc Auto V2

The refreshed interface uses shared Thymeleaf fragments and local CSS/JavaScript, with no external UI CDN. The catalogue and vehicle details are public; reservations, history and profile actions require the current authenticated session.

- Combined brand/model, minimum-seat, maximum-price and availability filters.
- A complete reservation flow with a live price estimate, server-side date/duration checks, confirmation, history and return.
- In the `demo` and `persistent` profiles, one-click access creates a different fictional CLIENT account for each visitor. Signup generates fictional identity references; the interface asks for no real documents or payment details.
- An atomic vehicle claim inside the reservation transaction prevents simultaneous reservations. Repeating a previous return cannot release a vehicle booked afterward.
- Project and FAQ pages explain the demonstration; public [privacy](https://khayem-car-rental.vercel.app/privacy) and [demo terms](https://khayem-car-rental.vercel.app/terms) pages describe its data handling and operating conditions.

Run the verification suite from `car-rental-client/` with `./mvnw test` (Windows: `./mvnw.cmd test`). It covers public rendering, protected routes, visitor isolation, role/id tampering, session rotation, combined filtering, invalid booking inputs, concurrent booking and the original login/reservation/return flow.

This remains a pedagogical demonstration. The public site uses PostgreSQL and shared JDBC sessions. The separate H2 `demo` profile remains in memory: its accounts and bookings can disappear when an instance restarts, and separate H2 instances do not share their database or sessions. See [the PostgreSQL setup and verification guide](docs/POSTGRESQL.md) for the durable profile. Do not point the resetting demo schema at a persistent database.

## Authentication and Google login

Local authentication now uses Spring Security, BCrypt with cost 12, CSRF tokens on state-changing forms, session rotation on login and POST-only logout. Profile password changes require the current password and confirmation, then end the current session. The one-click demo visitor remains isolated and needs no personal data.

Google OpenID Connect is **active on the public demo**, with the real callback and subsequent login verified on 26 October 2026. It requests only `openid`, `profile` and `email`, without Gmail or Drive access. A Google identity uses its verified provider/subject identifier; matching an email address never silently links an existing local account.

For another clone or deployment, Google login remains conditional: without both `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, the Google button and OAuth routes are absent. Read [the setup and migration guide](docs/GOOGLE-LOGIN.md) before configuring that deployment's client and callback. Keep credentials server-side.

Automated verification on **25 October 2026**: 20 tests passed with real disposable PostgreSQL enabled, covering the existing local/security flows, URL conversion, data and session retention across application instances/restarts, and concurrent booking with one winner. After adding the privacy and terms pages, 19 selected rendering, security, mocked Google, rental-flow and URL tests passed and the packaged build succeeded. The real public Google flow described above was verified separately on 26 October.

`scripts/verify-live.ps1` exercises rendered CSRF forms, isolated visitor access, booking, history, return and logout; it releases its fictional test vehicle afterward.
