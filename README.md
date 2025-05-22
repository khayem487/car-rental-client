# Car Rental Client Application

A web-based car rental system for clients to browse available cars, make reservations, and manage their rental history.

## Technologies Used

- Backend: Java + Spring Boot + JDBC
- Database: Oracle DB
- Frontend: Thymeleaf + CSS + JavaScript
- Build Tool: Maven

## Features

- User authentication (login/registration)
- Browse available cars with filtering options
- Car rental reservation system
- User profile management
- Rental history tracking
- Responsive design

## Setup Instructions

1. Database Configuration:
   - Oracle DB with username="c##loc", password="loc"
   - Run `schema_and_data.sql` to create and populate the database

2. Run the Application:
   ```bash
   # Change directory to car-rental-client
   cd car-rental-client
   
   # Compile the project
   ./mvnw clean package
   
   # Start the application
   ./mvnw spring-boot:run
   ```

3. Access the application at `http://localhost:8080`

## Project Structure

This application is the client-facing part of a car rental system. The database is shared with a separate administrative application that manages aspects like penalties and car maintenance.

## License

[MIT License](LICENSE) 