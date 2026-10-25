# Car Rental Client Application

A web-based car rental system for clients to browse available cars, make reservations, and manage their rental history.

## Technologies Used

- Backend: Java 17 + Spring Boot 4.1 + Spring Security + JDBC
- Frontend: Thymeleaf + CSS + JavaScript
- Databases:
  - Oracle DB (original stack)
  - H2 in-memory (demo profile for free cloud hosting)
  - PostgreSQL (explicit `persistent` profile with Flyway migrations and shared JDBC sessions)
- Build Tool: Maven

## Features

- User authentication (login/registration, BCrypt passwords and CSRF protection)
- Optional Google OpenID Connect login, enabled only when its server credentials are configured
- Browse available cars with filtering options
- Car rental reservation system
- User profile management
- Rental history tracking
- Responsive design

## Free Cloud Demo (Render)

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

This application is the client-facing part of a car rental system. The database can be Oracle (full setup) or H2 (demo profile).

## License

[MIT License](LICENSE)

<!-- trigger linguist rescan -->

## Real application on Vercel

The same Spring Boot / JDBC / Thymeleaf application is available at:

**https://khayem-car-rental.vercel.app/**

`Dockerfile.vercel` builds the Java application with Maven and runs the existing `demo` Spring profile. It uses the original seeded H2 database, real login sessions, catalogue queries, reservations and rental history. The forwarded-header setting preserves HTTPS redirects behind the Vercel proxy.

```sh
npx vercel@64.7.0 deploy --prod
```

H2 is in memory, as in the original Render demonstration: a restarted instance begins with the seed data. Reservations are demonstration data, and no payment is collected. Vercel containers scale down after five minutes without traffic; the public URL remains available, with a possible cold start. This is not a promise of a continuously running JVM.

Verification on 20 October 2026: the local `DemoRentalFlowTest` and deployed `scripts/verify-live.ps1` passed login, catalogue, reservation, history, return and logout using the existing fictional demo account. The public test released its vehicle afterward. A measured initial login request took 1.45 seconds; this single observation is not an uptime or latency guarantee.

```powershell
./scripts/verify-live.ps1 -BaseUrl https://khayem-car-rental.vercel.app
```

Render configuration is retained as a fallback. The Vercel project is connected to this GitHub repository: a push to `master` triggers a production build. Direct authenticated Vercel CLI publication also works.

## Loc Auto V2

The refreshed interface uses shared Thymeleaf fragments and local CSS/JavaScript, with no external UI CDN. The catalogue and vehicle details are public; reservations, history and profile actions require the current authenticated session.

- Combined brand/model, minimum-seat, maximum-price and availability filters.
- A complete reservation flow with a live price estimate, server-side date/duration checks, confirmation, history and return.
- In the `demo` profile, one-click access creates a different fictional CLIENT account for each visitor. Signup generates fictional identity references; the interface asks for no real documents or payment details.
- An atomic vehicle claim inside the reservation transaction prevents simultaneous reservations. Repeating a previous return cannot release a vehicle booked afterward.
- Completed project and FAQ pages replace the old placeholder contact links.

Run the verification suite from `car-rental-client/` with `./mvnw test` (Windows: `./mvnw.cmd test`). It covers public rendering, protected routes, visitor isolation, role/id tampering, session rotation, combined filtering, invalid booking inputs, concurrent booking and the original login/reservation/return flow.

This remains a pedagogical demonstration. The default H2 `demo` profile is in memory: accounts and bookings can disappear when an instance restarts, and separate instances do not share its database or sessions. The separate `persistent` profile now supports PostgreSQL and shared JDBC sessions; follow [the PostgreSQL setup and verification guide](docs/POSTGRESQL.md) before enabling it. Do not point the resetting demo schema at a persistent database.

## Authentication and Google login

Local authentication now uses Spring Security, BCrypt with cost 12, CSRF tokens on state-changing forms, session rotation on login and POST-only logout. Profile password changes require the current password and confirmation, then end the current session. The one-click demo visitor remains isolated and needs no personal data.

Google OpenID Connect is implemented but is **inactive on the public demo until its OAuth credentials are configured**. Without both `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`, the Google button and OAuth routes are absent. Read [the setup and migration guide](docs/GOOGLE-LOGIN.md) before enabling it. A Google identity uses its verified provider/subject identifier; matching an email address never silently links an existing local account.

Verification on 22 October 2026: 17 automated tests passed, including local login, CSRF rejection, password hashing/change, visitor isolation, rental concurrency and mocked Google claims. The real Google consent/callback flow still needs a configured OAuth client and a browser verification. `scripts/verify-live.ps1` exercises the rendered CSRF forms, demo access, booking, history, return and logout; it releases its fictional test vehicle afterward.
