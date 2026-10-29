---
description: "Implementation tasks for Spring Security admin login"
---

# Tasks: Spring Security for Admin Login

**Input**: Design documents from `specs/003-spring-security-admin-login/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/admin-authentication-api.md](./contracts/admin-authentication-api.md)

**Tests**: Test tasks are included because the specification defines independent tests and acceptance scenarios for each user story.

**Organization**: Tasks are grouped by user story and ordered by the security flow dependencies: sign-in, authorization, then session termination and expiry.

## Phase 1: Setup

**Purpose**: The existing Spring Boot and React projects, Spring Security dependency, packages, and test framework are already present. No project initialization or dependency changes are required.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preserve persisted administrator data and add the shared session and audit storage required by the stories.

- [X] T001 Configure `backend/src/main/resources/application.properties`, `backend/src/main/resources/application-development.properties`, and `backend/src/main/resources/application-production.properties` so development preserves database rows and production validates a schema managed by migrations.
- [X] T002 [P] Restrict known teller/manager seed accounts in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/DataSeeder.java` to the explicit development profile; production startup must not create predictable default administrator credentials.
- [X] T003 [P] Add `AdminSecurityEvent` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/AdminSecurityEvent.java` with a required event category, occurrence timestamp, safe outcome, optional safe subject, and nullable administrator relationship; do not store passwords, bearer tokens, or request bodies.
- [X] T004 Add `AdminSecurityEventRepository` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/AdminSecurityEventRepository.java` for saving and querying security events; depend on T003.
- [X] T005 Update `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/Session.java` to persist a required unique one-way bearer-token digest instead of a raw token, while retaining the owner, active state, creation time, last-used time, and expiry.
- [X] T006 Update `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/SessionRepository.java` to find administrator sessions by token digest and support revocation and active-session replacement; depend on T005.

**Checkpoint**: Existing account rows survive restarts, seeded credentials are development-only, and session/audit persistence can support authentication without storing reusable token secrets.

---

## Phase 3: User Story 1 - Sign in as an administrator (Priority: P1)

**Goal**: Authenticate eligible tellers and managers through the existing login API, upgrade legacy credentials after successful verification, and issue a session only after all persistence succeeds.

**Independent Test**: Sign in with valid teller and manager credentials and confirm the existing success fields are returned; compare unknown-user and wrong-password failures; verify missing credentials, lockout, first-success credential upgrade, and upgrade-persistence failure never create an unauthorized session.

### Tests for User Story 1

- [X] T007 [P] [US1] Extend `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Service/AuthServiceTests.java` to cover username/email login, teller and manager eligibility, equivalent unknown-user/wrong-password failures, missing credentials, five-failure temporary lockout and automatic expiry, failure-count reset, token digest storage, and session replacement.
- [X] T008 [P] [US1] Add login API and credential-upgrade integration cases in `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Config/AdminAuthSecurityTests.java`, including successful response fields, first-success legacy upgrade, and no token in error responses; verify upgrade-persistence failure creates no session in `AuthServiceTests.java`.

### Implementation for User Story 1

- [X] T009 [P] [US1] Implement the explicit legacy-aware password comparison and adaptive password upgrade policy in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Security/LegacyAwarePasswordEncoder.java`; accept the existing legacy format only for verification and require successful upgrade before login can complete.
- [X] T010 [P] [US1] Implement administrator-only user lookup in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Security/AdminUserDetailsService.java` using the existing username-or-email repository query; expose only enabled teller/manager authorities and keep customer identities out of this lookup.
- [X] T011 [US1] Configure an admin-scoped `DaoAuthenticationProvider` and `AuthenticationManager` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/AdminSecurityConfig.java` using T009 and T010; do not share the customer provider or customer session context.
- [X] T012 [US1] Update `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AuthService.java` to use authenticated administrator details, record login attempts, enforce five consecutive failures for 15 minutes without setting administrative `enabled=false`, clear expired temporary locks, and issue a single active session only after password upgrade and session persistence succeed.
- [X] T013 [US1] Add `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AdminSecurityEventService.java` and record safe successful-login and lockout events through `AdminSecurityEventRepository`; never include passwords or bearer tokens in event subjects or outcomes.
- [X] T014 [US1] Update `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/AuthController.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/AuthResponse.java` to preserve `POST /api/auth/login` request and success fields (`token`, `username`, `role`, `message`), return the raw token only on successful login, and produce safe validation/authentication/locked-account errors.

**Checkpoint**: Existing teller and manager accounts can sign in; unknown users and bad passwords are indistinguishable; a failed credential upgrade or persistence operation cannot result in a session.

---

## Phase 4: User Story 2 - Protect administrator operations (Priority: P1)

**Goal**: Enforce administrator authentication and role eligibility on every existing customer and account management operation without changing the customer-authentication chain.

**Independent Test**: Use a valid teller or manager bearer token on protected operations and confirm success; repeat without a token, with malformed/expired/revoked tokens, and with a customer session, confirming rejection before data access or mutation and safe audit events for denials.

### Tests for User Story 2

- [X] T015 [P] [US2] Extend `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Config/AdminAuthSecurityTests.java` to verify protected `/customer/**` operations require an active teller/manager token, reject missing/malformed/expired/revoked/disabled/non-admin credentials, return 403 when an active session's role changes to a non-admin role, and return safe errors without exposing data.
- [X] T016 [P] [US2] Extend `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthSecurityTests.java` to prove `/api/customer-auth/**` retains its existing CSRF, cookie-session, and customer-principal behavior after adding the admin chain.

### Implementation for User Story 2

- [X] T017 [US2] Implement `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Security/AdminBearerTokenFilter.java` to parse the existing bearer header, hash the presented token, load the matching session digest, verify active/expiry/owner-enabled/eligible-role state, refresh last-used and 15-minute idle expiry, and populate a request-scoped Spring Security principal without using `HttpSession`.
- [X] T018 [US2] Complete the ordered admin `SecurityFilterChain` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/AdminSecurityConfig.java`: leave the customer chain first, match `/api/auth/**`, `/customer/**`, and `/account/**` before the permissive fallback, make admin authentication stateless, allow auth-controller routes to reach their handlers, and require an eligible administrator for protected operations without changing customer CSRF/CORS policy.
- [X] T019 [US2] Add JSON authentication-entry-point and access-denied handling in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/ApiExceptionHandler.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config/AdminSecurityConfig.java`; return safe 401/403 error/message fields without exposing account existence, internal failures, or bearer tokens.
- [X] T020 [US2] Record denied administrator operations through `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AdminSecurityEventService.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/AdminSecurityEventRepository.java`, including safe identity context where known and no request bodies or session secrets.

**Checkpoint**: Protected customer/account APIs enforce backend authorization, and customer registration/login/CSRF behavior remains unchanged.

---

## Phase 5: User Story 3 - End and expire administrator sessions (Priority: P2)

**Goal**: Make logout, replacement, disablement, and 15-minute inactivity invalidate administrator access while leaving customer authentication state untouched.

**Independent Test**: Sign in and verify access, sign out, then verify the token is unusable; separately advance beyond 15 minutes of inactivity and verify denial. Confirm repeated logout is safe and customer session state is unchanged.

### Tests for User Story 3

- [X] T021 [P] [US3] Extend `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Service/AuthServiceTests.java` and `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Config/AdminAuthSecurityTests.java` to cover logout, repeated logout, single-session replacement, 15-minute inactivity expiry, disabled-owner invalidation, and customer-session isolation.
- [X] T022 [P] [US3] Extend `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Config/AdminAuthSecurityTests.java` to verify session validation returns `token: null`, logout returns the existing safe response fields, and expired/revoked tokens cannot reach protected operations.

### Implementation for User Story 3

- [X] T023 [US3] Update `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AuthService.java` to revoke the active administrator session on logout, preserve idempotent repeated logout, expire sessions after 15 minutes idle, invalidate sessions for disabled owners, and replace any prior active session on successful login.
- [X] T024 [US3] Update `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/AuthController.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/AuthResponse.java` so `GET /api/auth/session` validates the bearer session but always returns `token: null`, while `POST /api/auth/logout` returns the existing signed-out response and never changes customer session state.
- [X] T025 [US3] Extend `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AdminSecurityEventService.java` to persist safe sign-out and session-expiry events without recording raw tokens or credentials.

**Checkpoint**: Explicit logout, inactivity expiry, account disablement, and session replacement all prevent subsequent administrator access without affecting customer sessions.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verify documentation, compatibility, security behavior, and the complete feature.

- [X] T026 [P] Reconcile the implemented response status/error behavior with `specs/003-spring-security-admin-login/contracts/admin-authentication-api.md` and the feature requirements in `specs/003-spring-security-admin-login/spec.md`.
- [X] T027 [P] Update `specs/003-spring-security-admin-login/quickstart.md` with the final development-profile startup, safe sample-account handling, and executable backend/frontend validation commands.
- [X] T028 Run the backend regression suite using `backend/pom.xml` and frontend regression/build checks using `frontend/package.json`; verify admin/customer backend scenarios, capture frontend test failures for follow-up, and confirm the frontend production bundle still builds.

**Validation note**: The complete backend suite passed (89 tests, 0 failures or errors), including the regression case for a role changed during an active session. The frontend production build passed. The frontend test suite was executed serially but has pre-existing failures in six test imports for missing account/customer page modules and three stale CustomerRegistration/CustomerHome assertions; no frontend source files were changed for this backend feature.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No initialization tasks; existing projects and dependencies are reused.
- **Foundational (Phase 2)**: Required before all stories; provides non-destructive data handling, digest-backed sessions, and audit persistence.
- **User Story 1 (Phase 3)**: Depends on Foundational; establishes the login and session-issuance flow.
- **User Story 2 (Phase 4)**: Depends on User Story 1 because the bearer-token filter consumes the sessions and admin identities issued by login.
- **User Story 3 (Phase 5)**: Depends on User Stories 1 and 2 because logout and expiry invalidate sessions created and enforced by those flows.
- **Polish (Phase 6)**: Depends on all selected stories.

### User Story Dependencies

- **US1 (P1)**: Starts after Foundational. No dependency on another user story.
- **US2 (P1)**: Depends on US1 session issuance and administrator identity.
- **US3 (P2)**: Depends on US1 session issuance and US2 token enforcement.

### Parallel Opportunities

- T002, T003, and T005 can be started independently after the foundational phase begins; T004 depends on T003 and T006 depends on T005.
- In US1, T007 and T008 can be written in parallel; T009 and T010 touch different files and can be implemented in parallel after the tests are defined.
- In US2, T015 and T016 can be written in parallel; filter, chain, error handling, and audit tasks touch distinct files but must be integrated in their dependency order.
- In US3, T021 and T022 can be written in parallel before lifecycle implementation.
- T026 and T027 are independent documentation updates.
- The user stories themselves are sequential because each later story relies on the authentication/session behavior introduced earlier.

## Parallel Example: User Story 1

```text
Task: T007 - Extend AuthServiceTests.java with login, lockout, and credential-upgrade cases
Task: T008 - Add AdminAuthSecurityTests.java login/API integration cases

After both test tasks are defined:
Task: T009 - Implement LegacyAwarePasswordEncoder.java
Task: T010 - Implement AdminUserDetailsService.java
```

## Parallel Example: User Story 2

```text
Task: T015 - Add protected-operation authorization cases to AdminAuthSecurityTests.java
Task: T016 - Add customer-chain regression cases to CustomerAuthSecurityTests.java
```

## Parallel Example: User Story 3

```text
Task: T021 - Add AuthSessionLifecycleTests.java
Task: T022 - Add session/logout API cases to AdminAuthSecurityTests.java
```

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Foundational tasks so persisted data is safe and token digests/audit events are supported.
2. Complete US1 and verify teller/manager login, equivalent credential failures, credential upgrade, lockout, and no session on failure.
3. Stop and validate the login API before proceeding; US1 alone does not make operational endpoints production-ready.

### Incremental Delivery

1. Complete US1 to establish safe administrator authentication and token issuance.
2. Complete US2 before exposing any protected operations, then validate every protected route and customer-chain isolation.
3. Complete US3 to establish logout, rolling inactivity expiry, and revocation semantics.
4. Complete cross-cutting regression and contract validation.

## Notes

- Every task uses the required checkbox, sequential task ID, optional parallel marker, story label where applicable, and concrete file path.
- Test tasks are ordered before their story's implementation tasks.
- [P] marks only tasks that can work independently on distinct files; later story phases intentionally depend on earlier authentication/session behavior.
