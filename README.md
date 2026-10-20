# Car Rental Client Application

A web-based car rental system for clients to browse available cars, make reservations, and manage their rental history.

## Technologies Used

- Backend: Java + Spring Boot + JDBC
- Frontend: Thymeleaf + CSS + JavaScript
- Databases:
  - Oracle DB (original stack)
  - H2 in-memory (demo profile for free cloud hosting)
- Build Tool: Maven

## Features

- User authentication (login/registration)
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

Render configuration is retained as a fallback. Git integration is optional; direct authenticated Vercel CLI publication works.
