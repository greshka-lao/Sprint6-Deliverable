# Quickstart: Admin Login Validation

## Prerequisites

- Java 17+
- Maven
- MySQL instance available for the Spring Boot application
- Node.js 20+
- Frontend dependencies installed via npm

## Setup

1. Start the MySQL database and ensure the application database is available.
2. Configure the backend database connection in the Spring Boot application properties.
3. Run database migrations or schema setup for the admin identity tables.
4. Seed at least one database-backed test admin user for validation.
5. Install frontend dependencies with npm install.

## Run the system

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Validation scenarios

1. Open the admin sign-in page in the browser.
2. Sign in with a valid database-backed test admin account.
3. Confirm redirection to the admin area and visibility of protected customer/account screens.
4. Attempt sign-in with invalid credentials and confirm the failure message.
5. Attempt access to a protected route without a valid session and confirm redirect to the sign-in page.
6. Log out and confirm the session is invalidated.
7. Allow the session to expire and confirm reauthentication is required.

## Expected outcomes

- Successful sign-in reaches the admin area.
- Invalid credentials display a clear validation or authentication failure message.
- Unauthorised access is blocked.
- Session expiry requires reauthentication.
- The application keeps the existing UI structure while distinguishing the login flow from protected views.
