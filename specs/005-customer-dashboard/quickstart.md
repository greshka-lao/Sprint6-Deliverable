# Quickstart: Customer Dashboard

This guide validates the customer dashboard end to end after implementation. The endpoint contract is in [contracts/customer-dashboard.openapi.yaml](contracts/customer-dashboard.openapi.yaml), and its response data is described in [data-model.md](data-model.md).

## Prerequisites

- Java 17 and the repository's Maven wrapper.
- Node.js and npm.
- The application's MySQL database configured and available using the existing backend configuration.
- Backend and frontend dependencies installed as described by their existing manifests.
- A customer account with a valid login, profile details, and at least two linked accounts with known balances. Have a second customer available for ownership-isolation checks.

## Run focused tests

From the repository root, run the backend dashboard security/integration tests:

```powershell
.\backend\mvnw.cmd -f backend\pom.xml "-Dtest=CustomerDashboardSecurityTests" test
```

Run the customer dashboard UI tests:

```powershell
Set-Location frontend
npm test -- --run src/__test__/CustomerHome.test.jsx
```

Run the frontend production build:

```powershell
npm run build
```

## Validate in the running application

1. Start the backend and frontend using the repository's existing run configuration.
2. Sign in at `/customer/login` as a customer with known profile data and linked accounts.
3. Confirm sign-in navigates to `/customer/home`.
4. Verify the dashboard shows the signed-in customer's name, email, address, and phone number.
5. Verify every account assigned to that customer appears with its identifier, account type, and balance.
6. Add the expected account balances and confirm that the displayed total matches to two decimal places in NZD.
7. Sign in as a different customer and confirm the first customer's details and accounts are not visible.
8. Sign out and open `/customer/home` directly; confirm the protected route redirects to customer sign-in and the dashboard endpoint rejects the unauthenticated request.
9. Repeat with a customer having no accounts and verify the empty state and zero total. Repeat with a missing profile field and verify the dashboard marks it unavailable.
10. Simulate an unavailable dashboard response; verify the UI reports an error and does not show stale values as current.

## Expected outcomes

- The protected dashboard endpoint returns only the profile and account data owned by the session principal.
- Unauthenticated requests receive an authorization failure.
- The total uses the exact accounts included in the response, shown at two decimal places.
- Empty data and request failure are distinct user-visible states.
- Existing admin routes and workflows continue to work.
