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
