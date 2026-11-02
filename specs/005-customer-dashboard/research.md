# Research: Customer Dashboard

## Decisions

### Use the existing customer authentication boundary for dashboard reads

- **Decision**: Add a read endpoint at `GET /api/customer-auth/dashboard`, within the existing customer authentication controller/security route.
- **Rationale**: The customer security chain already matches `/api/customer-auth/**` and requires `ROLE_CUSTOMER` for requests other than explicitly permitted authentication/session operations. The existing customer frontend request helper already includes credentials. The legacy `/customer` and `/account` controllers use admin session validation and must not be used to serve customer dashboard data.
- **Alternatives considered**: Calling the legacy customer/account endpoints from the browser, which does not provide the right customer authorization boundary; adding a separate security chain, which duplicates existing authentication behavior.

### Derive ownership from the authenticated principal

- **Decision**: Resolve the customer using the authenticated session's principal name and the existing `CustomerRepository.findByUsername` lookup. The dashboard request accepts no customer ID.
- **Rationale**: The existing login/session flow authenticates the username and stores that identity in Spring Security's session. Resolving ownership on the server prevents a caller from selecting another customer's profile or accounts.
- **Alternatives considered**: Accepting `customerId` as a query or path parameter, which introduces an avoidable object-ownership authorization risk; trusting a customer ID supplied by the browser.

### Return a purpose-built dashboard response

- **Decision**: Return a DTO with profile values, account identifiers/types/balances, and a combined total; do not serialize JPA entities directly.
- **Rationale**: `Customer` and `Account` are persistence entities with inheritance and associations, and several properties are intentionally ignored or transient in JSON. A dashboard response gives the UI only the values it needs and avoids exposing persistence details.
- **Alternatives considered**: Returning a `Customer` plus account entities, which couples the API to entity serialization and can disclose fields or create lazy-loading/recursive-serialization problems; reusing the admin list DTO, which omits profile contact information and accounts.

### Preserve the existing single-currency display convention

- **Decision**: Represent account and total amounts in NZD in the dashboard response and display amounts with the existing two-decimal dollar convention. Do not introduce currency conversion or a currency database field.
- **Rationale**: The persisted account model contains only a `double balance` and no currency code. Existing admin account screens format balances with the `en-NZ` locale, a `$` prefix, and two fractional digits. The feature specification assumes accounts in one total share a currency; NZD is the narrowest consistent platform assumption supported by the current UI.
- **Alternatives considered**: Adding currency fields and migration, which expands this read-only dashboard feature into multi-currency account-model work; inferring different currencies from address locations, which is unsupported by account data; converting balances, for which no rates or conversion behavior exist.

### Compute the displayed total from the account balances without changing persistence

- **Decision**: Sum each retrieved account's existing stored balance using decimal arithmetic at the response/service boundary and expose the result at currency precision; do not change the persisted balance type in this feature.
- **Rationale**: Account creation and storage currently use `double`, while the existing interface accepts and displays two decimal places. Decimal aggregation avoids introducing further binary floating-point error in the combined amount without requiring a data migration.
- **Alternatives considered**: Summing with primitive floating-point arithmetic, which can introduce visible cent-level artifacts; converting the entity and database column to a decimal type, which changes account persistence beyond this dashboard's scope.

### Load the customer overview as one read and render explicit states

- **Decision**: Load profile and accounts together through the dashboard response. In the existing customer page, distinguish initial loading, request failure, an empty account list, and missing profile values.
- **Rationale**: A single read keeps profile and account information from being presented as if it were a consistent snapshot assembled from separate requests. Explicit states satisfy the specification's empty and unavailable-data requirements.
- **Alternatives considered**: Separate profile and account calls, which can fail or refresh independently and complicate the ownership boundary; treating failed or missing values as zero or blank without explanation, which can misrepresent account data.

## Repository Evidence

- `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/CustomerSecurityConfig.java`: `/api/customer-auth/**` is a dedicated customer session security chain; unmatched requests in that chain require `ROLE_CUSTOMER`.
- `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthController.java`: customer login establishes the session principal and the session endpoint resolves its customer by principal name.
- `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/CustomerRepository.java`: `findByUsername` already provides the customer lookup needed for ownership resolution.
- `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/Customer.java`: profile fields, address relationship, and customer account relationship are already modeled.
- `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/Account.java`: accounts expose an identifier and balance, link to a customer, and have no currency field.
- `frontend/src/services/customerAuthService.js`: customer API requests use `credentials: 'include'` and share the existing customer-auth base URL.
- `frontend/src/pages/customer/CustomerHome.jsx`: the protected customer landing page currently renders a placeholder inside the existing header/sidebar layout.
- `frontend/src/pages/admin/account/Accounts.jsx` and `frontend/src/components/AccountPopUp.jsx`: current account balances use `$`, `en-NZ`, and two decimal places.
- `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthSecurityTests.java` and `frontend/src/__test__/CustomerHome.test.jsx`: existing integration and UI test locations/patterns.

## Technical Choices

- No new runtime or test dependencies.
- No schema changes; reuse customer/address/account persistence and the existing repository lookup.
- Keep the dashboard API read-only and use the server session as the only ownership source.
- Reuse existing customer header, sidebar, protected route, service conventions, and test runners.
