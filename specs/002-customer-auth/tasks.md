# Tasks: Customer Authentication

**Input**: Design documents from `specs/002-customer-auth/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/customer-authentication-api.md`, `quickstart.md`

**Tests**: No separate test-first tasks are listed because the feature specification does not request TDD. Implementation must preserve coverage through the existing Maven and Vitest suites and the final quickstart validation.

**Organization**: Tasks are grouped by user story after shared setup and foundational work. User stories are listed in priority order from `spec.md`.

## Phase 1: Setup

**Purpose**: Add the planned Spring Security dependency using Spring Boot dependency management.

- [X] T001 Add `spring-boot-starter-security` to `backend/pom.xml` without overriding the Spring Boot-managed version.

---

## Phase 2: Foundational

**Purpose**: Establish the customer identity persistence and security boundaries, and clear the `/customer/**` namespace for customer-facing pages. These prerequisites block the user-story work.

- [X] T002 [P] Move existing admin UI route declarations from `/login`, `/dashboard`, `/customer/**`, and `/account/**` to the corresponding `/admin/**` routes in `frontend/src/App.jsx`, leaving `/customer/**` available for customer pages.
- [X] T003 [P] Create `CustomerAccount` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerAccount.java` with these constraints: `name`: required customer name as entered; `username`: generated, unique customer sign-in name; `address`: required structured postal address using `streetNumber`, `postalCode`, `city`, and `province`; `email`: normalized, unique customer contact address; `phoneNumber`: required customer contact phone number stored in international E.164 format, including country code; `passwordHash`: algorithm-prefixed one-way encoded password produced by Spring Security `PasswordEncoder`; `role`: server-assigned `CUSTOMER`; `status`: self-registration creates an `ACTIVE` account; `lastLoginAt`: nullable until first login. Do not relate it to operational `Customer` or `Account`.
- [X] T004 Create `CustomerAccountRepository` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/CustomerAccountRepository.java` with normalized-email and username lookups, relying on database uniqueness constraints for both values.
- [X] T005 Create DTOs in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerRegistrationRequest.java`, `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerRegistrationResponse.java`, `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerLoginRequest.java`, `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerAuthResponse.java`, `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerSessionResponse.java`, and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerCsrfTokenResponse.java`, matching `specs/002-customer-auth/contracts/customer-authentication-api.md`; reject client-supplied username, role, status, admin flag, session identifier, and operational customer/account IDs.
- [X] T006 Configure `SecurityFilterChain` instances in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/CustomerSecurityConfig.java`: scope the higher-priority chain to customer authentication APIs, require `ROLE_CUSTOMER` for protected customer endpoints, permit only the specified anonymous endpoints, and provide a fallback chain preserving current non-customer endpoint behavior without a generated Boot default user.
- [X] T007 Configure customer-chain CSRF, credentialed CORS restricted to configured frontend origins, session-fixation protection, and cookie policy in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/CustomerSecurityConfig.java`; enforce 15 minutes of customer-session inactivity without changing existing admin session behavior, using `backend/src/main/resources/application.properties` only if its setting can be scoped safely.

**Checkpoint**: Persistence types, security boundaries, and distinct UI route namespaces are ready for story implementation.

---

## Phase 3: User Story 1 - Create a customer account (Priority: P1)

**Goal**: Allow a prospective customer to submit required registration details and receive an account with a generated username.

**Independent Test**: Register a customer from the public entry flow, confirm the account is created with the generated username displayed, then sign in using that username and password without admin intervention.

### Implementation for User Story 1

- [X] T008 [P] [US1] Implement `CustomerUsernameService` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerUsernameService.java` to lowercase the name, remove all whitespace while preserving other characters, and append a per-base sequence starting at `001`, padded to at least three digits and expanding beyond `999`; handle concurrent collisions using the database unique constraint and bounded retry.
- [X] T009 [US1] Implement customer registration in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerAuthService.java`: normalize email consistently before lookup and persistence; enforce syntactic email validation and application field-length limits; validate required name, address, E.164 phone, and password; enforce an 8-character minimum, support passwords of at least 64 characters, and impose no arbitrary composition rules; assign `CUSTOMER` and `ACTIVE`; hash passwords with Spring Security `DelegatingPasswordEncoder`; do not create or link operational bank records.
- [X] T010 [US1] Implement `POST /api/customer-auth/register` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthController.java` to return `201` with the generated username, `400` for invalid input, and `409` with a clear duplicate-email response; ensure uniqueness violations from concurrent registration do not return duplicate usernames.
- [X] T011 [P] [US1] Implement the customer-auth API client in `frontend/src/services/customerAuthService.js` for CSRF bootstrap and registration, sending browser credentials and the `X-CSRF-TOKEN` header as specified in `specs/002-customer-auth/contracts/customer-authentication-api.md`.
- [X] T012 [US1] Create the customer registration form in `frontend/src/pages/auth/CustomerRegistration.jsx` with required name, street number, postal code, city, province, email, E.164 phone number, and password fields; display accessible validation, duplicate-email feedback, and the returned generated username.
- [X] T013 [US1] Register `/customer/register` in `frontend/src/App.jsx` and add customer registration and sign-in entry links at `/` without placing customer links inside admin-only navigation.

**Checkpoint**: A customer can register from the public entry flow and see their generated username; the account is separate from admin and operational banking records.

---

## Phase 4: User Story 2 - Sign in as a customer (Priority: P1)

**Goal**: Let a registered customer sign in, maintain a customer-only session, and access the customer home page.

**Independent Test**: Create or register a customer account, sign in with its username and password, confirm navigation to the customer home page, and verify invalid credentials remain a generic failure on the sign-in page.

### Implementation for User Story 2

- [X] T014 [US2] Implement a customer-only `UserDetailsService`/`DaoAuthenticationProvider` and adaptive `PasswordEncoder` bean in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/CustomerSecurityConfig.java`; authenticate only `CustomerAccount` records and tune the adaptive encoder for the target runtime.
- [X] T015 [US2] Implement `POST /api/customer-auth/login`, `GET /api/customer-auth/session`, and `POST /api/customer-auth/logout` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthController.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerAuthService.java`; return the documented response shapes, rotate the session identifier on login, return the same generic `401` for unknown usernames and wrong passwords, and ensure customer logout does not mutate admin state.
- [X] T016 [US2] Add temporary failed-sign-in rate limiting in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerAuthService.java` and return the documented generic `429` response with `Retry-After`; do not permanently disable or lock accounts and do not disclose account existence.
- [X] T017 [US2] Implement customer session and CSRF API handling in `frontend/src/services/customerAuthService.js`, `frontend/src/context/CustomerAuthProvider.jsx`, `frontend/src/context/CustomerAuthValue.js`, and `frontend/src/context/useCustomerAuth.js`, including credentialed requests, session restoration, fresh CSRF-token retrieval when needed, logout, and session-expiry handling without reusing the admin auth context.
- [X] T018 [US2] Create the customer sign-in page in `frontend/src/pages/auth/CustomerLogin.jsx` with username/password inputs, accessible generic authentication errors, temporary-rate-limit feedback, and navigation to `/customer/home` after successful authentication.
- [X] T019 [US2] Create the customer home page and customer-only route guard in `frontend/src/pages/customer/CustomerHome.jsx` and `frontend/src/components/CustomerProtectedRoute.jsx`; require a validated `ROLE_CUSTOMER` session and redirect expired or anonymous users to `/customer/login`.

**Checkpoint**: Customer login, home access, session validation, logout, and temporary rate limiting work independently of admin authentication.

---

## Phase 5: User Story 3 - Maintain separation between admin and customer access (Priority: P2)

**Goal**: Preserve admin workflows while ensuring customer accounts and sessions cannot use admin UI or protected admin operations.

**Independent Test**: Verify the admin UI resolves under `/admin/**`, customer UI resolves under `/customer/**`, customer credentials cannot authenticate as admin, a customer session is denied admin-only access, and existing admin sign-in/API behavior remains intact.

### Implementation for User Story 3

- [X] T020 [US3] Implement an admin-only route guard in `frontend/src/components/AdminProtectedRoute.jsx` and apply it to `/admin/**` routes in `frontend/src/App.jsx`, keeping it distinct from `CustomerProtectedRoute` and the existing admin auth context.
- [X] T021 [US3] Update admin page navigation and in-app links to the `/admin/**` namespace in `frontend/src/components/Header.jsx`, `frontend/src/components/Sidebar.jsx`, `frontend/src/pages/auth/Login.jsx`, `frontend/src/pages/admin/AdminDashboard.jsx`, `frontend/src/pages/customer/Customers.jsx`, `frontend/src/pages/customer/AddCustomer.jsx`, `frontend/src/pages/customer/UpdateCustomer.jsx`, `frontend/src/pages/account/Accounts.jsx`, `frontend/src/pages/account/AddAccount.jsx`, and `frontend/src/pages/account/UpdateAccount.jsx`.
- [X] T022 [US3] Verify the customer security chain rejects customer principals from admin-protected APIs while the fallback chain preserves existing admin authentication and API behavior; make any required scoped security adjustments in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/CustomerSecurityConfig.java` without changing the existing admin auth contract in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/AuthController.java`.

**Checkpoint**: Admin and customer routes, navigation, identities, sessions, and backend access remain separate.

---

## Phase 6: Polish & Cross-Cutting Validation

**Purpose**: Confirm the completed feature matches the design and existing project behavior.

- [X] T023 Update `specs/002-customer-auth/quickstart.md` only where implementation details require it, retaining the documented registration, username generation, validation, rate-limit, CSRF/session, route-separation, and admin-regression scenarios.
- [X] T024 Run the backend Maven test suite from `backend/` and the frontend Vitest suite from `frontend/`; resolve feature-related regressions and execute the end-to-end scenarios in `specs/002-customer-auth/quickstart.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 must complete before Spring Security configuration work.
- **Foundational (Phase 2)**: Depends on Setup; establishes the identity model, customer API boundary, and route namespace before story work begins.
- **User Stories (Phases 3-5)**: Depend on the foundational phase. US1 and US2 share the customer account and authentication boundary; complete account registration before validating sign-in with a newly registered account. US3 depends on the route migration in the foundation and verifies the completed customer/admin flows.
- **Polish (Phase 6)**: Depends on the desired user stories being complete.

### User Story Dependencies

- **US1 (P1)**: Starts after the foundation; provides customer registration and generated credentials.
- **US2 (P1)**: Starts after the foundation; depends on a customer account for the full registration-to-sign-in journey, but its sign-in behavior can be implemented and checked using a seeded customer account.
- **US3 (P2)**: Starts after the foundation; its admin/customer separation checks are strongest after US1 and US2 are implemented.

### Parallel Opportunities

- After T001, T002, T003, and T006 can proceed in parallel because they change separate frontend route, backend entity, and security configuration files.
- Within US1, T008 and T011 can proceed in parallel; T009 depends on T003/T004 and T008; T010 depends on T009; T012 depends on T011; T013 depends on T012.
- Within US2, T014 follows T006 and T003; T015 follows T014; T016 can proceed alongside frontend work after T015; T017 and T018 can proceed in parallel after the API contract is implemented; T019 follows T017.
- Within US3, T020 and T021 can proceed in parallel after US1/US2 routing and auth components exist; T022 can proceed independently after the security chains are in place.
- The final quickstart and suite run in T024 follows all desired story tasks.

## Parallel Execution Examples

### Foundation

```text
T002: Move admin UI routes to /admin/** in frontend/src/App.jsx and frontend/src/components/Sidebar.jsx
T003: Create CustomerAccount in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerAccount.java
T006: Configure scoped customer and fallback security chains in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/CustomerSecurityConfig.java
```

### User Story 1

```text
T008: Implement generated username allocation in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerUsernameService.java
T011: Implement the customer-auth API client in frontend/src/services/customerAuthService.js
```

### User Story 2

```text
T017: Implement customer auth/session client state in frontend/src/context/CustomerAuthProvider.jsx, frontend/src/context/CustomerAuthValue.js, and frontend/src/context/useCustomerAuth.js
T018: Create customer sign-in form in frontend/src/pages/auth/CustomerLogin.jsx
```

### User Story 3

```text
T020: Implement admin route guard in frontend/src/components/AdminProtectedRoute.jsx
T021: Update admin navigation and in-app route links to /admin/** in frontend pages and shared components
```

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Setup and Foundational phases.
2. Complete US1 registration and generated-username handling.
3. Complete the US2 sign-in capability needed to validate that a newly registered customer can sign in.
4. **STOP and VALIDATE** the registration-to-sign-in journey using the independent test criteria for US1.

### Incremental Delivery

1. Complete Setup + Foundational → customer identity, security boundaries, and route namespaces ready.
2. Add US1 → validate registration, unique username generation, and duplicate-email feedback.
3. Add US2 → validate customer sign-in, session lifecycle, logout, and temporary rate limiting.
4. Add US3 → validate role-specific UI/API separation and existing admin behavior.
5. Run the existing backend/frontend suites and all documented quickstart scenarios after each complete delivery increment.
