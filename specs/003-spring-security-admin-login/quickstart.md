# Quickstart: Admin Spring Security Validation

## Prerequisites

- Java 17 and Maven wrapper support.
- MySQL configured for the backend using the existing application settings.
- Node.js and npm for the existing frontend.
- Existing project dependencies installed.

## Run locally

In PowerShell from the repository root, start the backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=development"
```

In a second terminal:

```powershell
cd frontend
npm run dev
```

Open the frontend and choose the admin sign-in flow. The explicit `development` profile supplies the teller and manager test accounts through `DataSeeder`. These predictable credentials are development-only and must never be seeded in a production profile. The development database setup preserves account rows across restarts so password upgrades remain effective. The `production` profile validates a schema prepared by the deployment's managed migration process.

## Automated validation

Run backend tests:

```powershell
cd backend
.\mvnw.cmd test
```

Run frontend regression tests:

```powershell
cd frontend
npm run test -- --run
```

The backend tests cover the API and security cases below, including first-success credential upgrade, no session on upgrade persistence failure, lockout expiry, role checks, bearer-token digest migration and revocation, and customer/admin filter-chain separation.

## End-to-end scenarios

1. Sign in with an enabled teller account. Confirm successful navigation to the admin area and that the successful login response includes the existing token, username, role, and message fields.
2. Verify the stored legacy credential is replaced with an adaptive protected credential only after valid password authentication. Confirm the login response token remains usable and only its digest is stored with the session.
3. Use the returned bearer token for an existing customer/account operation. Confirm the authorized operation succeeds.
4. Repeat the protected operation with no token, a malformed token, a revoked token, an expired token, and a customer session only. Confirm each request is rejected and does not return or mutate protected data.
5. Validate the session and confirm the response retains the existing token field as `null` rather than echoing the bearer secret. Sign out. Confirm logout returns the existing message shape, the token is unusable afterward, and any separate customer session remains unaffected.
6. Submit invalid credentials for unknown usernames and known usernames with wrong passwords. Confirm both produce the same public authentication failure and create no session.
7. Submit five consecutive bad passwords for an otherwise enabled admin account. Confirm the temporary lock lasts 15 minutes, then expires automatically and resets its failure count. Confirm an administratively disabled account remains disabled after that interval.
8. Cause a legacy credential upgrade to fail persistence in a controlled integration test. Confirm login fails and no usable session token is created.
9. Sign in as both teller and manager. Confirm both roles can use the existing admin operations and a non-admin or customer identity cannot.
10. Use customer registration/login and CSRF-protected requests after the admin security change. Confirm the customer chain and its session behavior remain unchanged.
11. Start the application with the `production` profile against a schema prepared by the approved migration process. Confirm startup validates the schema and does not create development administrator accounts.

## Expected outcomes

- Admin endpoint paths, request fields, success response fields, and bearer-token usage remain compatible with the current admin client.
- Only active bank teller and bank manager accounts authenticate for admin access.
- A legacy credential is upgraded only after successful verification; failure to persist the upgrade fails closed.
- The five-failure lock automatically expires after 15 minutes without re-enabling administratively disabled users.
- Admin sessions enforce 15 minutes of inactivity and are invalidated on logout or replacement by another successful sign-in.
- Unauthenticated, expired, disabled, or non-admin requests cannot access customer/account operations.
- Customer authentication continues to use its separately scoped session and CSRF behavior.
- Legacy persisted administrator session token values are migrated in place to digests; already digested values remain unchanged on subsequent startups.
