# Quickstart: Customer Authentication Validation

## Prerequisites

- Java 17+
- MySQL configured for the backend using the existing application configuration
- Node.js and npm compatible with the repository's Vite setup
- Project dependencies installed (`backend` Maven wrapper and `frontend` npm dependencies)

## Run the application

In PowerShell, start the backend from the repository root:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

In a second terminal, start the frontend:

```powershell
cd frontend
npm run dev
```

Open the Vite URL (normally `http://localhost:5173`).

## Automated validation

From the repository root:

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
npm run test -- --run
```

Backend coverage should include customer registration persistence, generated username sequencing (including concurrent duplicate-name registration), unique email constraints, structured address and E.164 phone validation, 8-character minimum and 64-character password support, Spring Security password encoding/authentication, generic credential failure, `429` rate limiting with `Retry-After` and no permanent account lockout, 15-minute session inactivity expiry/logout, CSRF enforcement, and rejection of customer sessions on admin-only APIs. Verify the configured customer filter chain protects only customer routes, the fallback chain preserves existing admin/API behavior, and no generated Boot default user is active. Frontend coverage should include entry links, required/invalid name/address/email/E.164 phone/password fields, registration success with the generated username displayed, duplicate email feedback, customer sign-in failure/success, route separation, and preserving admin session behavior.

## End-to-end scenarios

1. Open `/`. Confirm it offers distinct customer sign-in, customer registration, and admin sign-in paths.
2. Select customer registration. Submit a name, street number, postal code, city, province, email, E.164 phone number, and password of at least 8 characters. Confirm success displays the generated username (for example, `greshkalao001`) and links to `/customer/login`; confirm no admin account or operational bank-customer record is created.
3. Register again with the same name and a different email. Confirm the next suffix is assigned (for example, `greshkalao002`). Try a previously used email and confirm a clear duplicate-email message and no second account.
4. Sign in at `/customer/login` with the generated username and password. Confirm navigation to `/customer/home` and a customer session.
5. Try invalid credentials and an admin-only route/API using the customer session. Confirm a generic sign-in failure and denied admin access; admin UI screens are under `/admin/**`.
6. Sign out from the customer flow. Confirm customer session validation fails while admin sign-in is at `/admin/login` and admin UI routes are under `/admin/**`.
7. Confirm the former admin customer/account management screens now resolve under `/admin/customers/**` and `/admin/accounts/**`, while `/customer/**` contains the customer-facing flow.
8. Leave a customer session inactive for 15 minutes and confirm that it expires. Make repeated failed sign-in attempts and confirm temporary rate limiting without permanent account lockout or account-existence disclosure.

## Expected outcomes

- Customer registration is self-service and creates a customer-only identity.
- Duplicate and malformed values receive clear, actionable feedback.
- Customer login establishes only a customer session; it does not authenticate or authorize admin access.
- Admin UI is consistently located under `/admin/**`; the existing admin API contract and authentication/session behavior remain unchanged.
- Forms use visible labels, suitable autocomplete values, and accessible error/success feedback.

For field rules and session state see [data-model.md](./data-model.md); for request/response shapes see [contracts/customer-authentication-api.md](./contracts/customer-authentication-api.md).
