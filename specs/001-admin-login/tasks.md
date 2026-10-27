# Tasks: Admin Login

**Input**: Design documents from `/specs/001-admin-login/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the shared auth structure and integrate it with the existing React and Spring Boot layout.

- [x] T001 Create the backend auth and session folders under backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller, Service, Repository, Model, and Config
- [x] T002 Create the frontend auth pages and shared route state under frontend/src/pages/auth, frontend/src/components, and frontend/src/context
- [x] T003 [P] Configure the existing application configuration files for the auth feature in backend/src/main/resources/application.properties and frontend/src/App.jsx

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish the core identity, session, and authorization foundation that all user stories depend on.

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel.

- [x] T004 Define the admin identity and session data model in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/AdminUser.java, Role.java, Session.java, and AccessAttempt.java
- [x] T005 [P] Add admin identity repositories in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/AdminUserRepository.java, RoleRepository.java, SessionRepository.java, and AccessAttemptRepository.java
- [x] T006 [P] Implement auth and authorization service foundations in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AuthService.java and AuthorizationService.java
- [x] T007 Implement the authentication endpoints and session validation contract in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/AuthController.java
- [x] T008 Add validation and exception handling for empty credentials, invalid credentials, expired sessions, and unauthorized access in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Exception and backend/src/main/java/com/fdmgroup/sprint4Deliverable/Config

---

## Phase 3: User Story 1 - Secure admin sign-in (Priority: P1) 🎯 MVP

**Goal**: Allow bank tellers and bank managers to sign in to the admin area with valid credentials and reach the protected admin interface.

**Independent Test**: Sign in with a valid database-backed test admin account and confirm the user reaches the admin area.

### Implementation for User Story 1

- [x] T009 [P] [US1] Create database-backed test admin seed records in backend/src/main/resources/data.sql or the equivalent application seed location
- [x] T010 [US1] Build the admin sign-in page and form UI in frontend/src/pages/auth/Login.jsx using the current Header and Sidebar styling patterns
- [x] T011 [US1] Add sign-in validation and form-state handling in frontend/src/pages/auth/Login.jsx and frontend/src/services/authService.js
- [x] T012 [US1] Implement login request processing and successful session creation in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AuthService.java and backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/AuthController.java
- [x] T013 [US1] Add the admin landing page and redirect flow in frontend/src/App.jsx and frontend/src/pages/admin/AdminDashboard.jsx
- [x] T014 [US1] Add user-facing success and failure messaging for valid and invalid sign-in attempts in frontend/src/pages/auth/Login.jsx

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently.

---

## Phase 4: User Story 2 - Protected admin access (Priority: P2)

**Goal**: Restrict access to customer and account management workflows so only authenticated admin users with the correct role can view or use them.

**Independent Test**: Attempt protected access without a valid session or without a valid admin role and confirm access is denied.

### Implementation for User Story 2

- [x] T015 [P] [US2] Add authenticated-user state management in frontend/src/context/AuthContext.jsx and frontend/src/services/authService.js
- [x] T016 [US2] Create a route guard for protected pages in frontend/src/components/ProtectedRoute.jsx
- [x] T017 [US2] Add authorization checks for admin-only requests in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AuthorizationService.java
- [x] T018 [US2] Enforce role-based access on the existing customer and account endpoints in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerController.java and AccountController.java
- [x] T019 [US2] Add unauthorized access messaging and redirect behavior in frontend/src/components/ProtectedRoute.jsx and frontend/src/pages/auth/Login.jsx

**Checkpoint**: At this point, User Stories 1 and 2 should both work independently.

---

## Phase 5: User Story 3 - Secure session handling (Priority: P3)

**Goal**: Keep the admin session secure, terminate it cleanly on sign-out, and require reauthentication after expiry or invalidation.

**Independent Test**: Sign out or allow a session to expire and confirm the system returns the user to a signed-out state requiring re-login.

### Implementation for User Story 3

- [x] T020 [P] [US3] Implement session expiry and inactivity rules in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/SessionService.java
- [x] T021 [US3] Implement logout and session invalidation in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/AuthController.java and backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AuthService.java
- [x] T022 [US3] Add the sign-out control and signed-out state handling in frontend/src/components/Header.jsx and frontend/src/context/AuthContext.jsx
- [x] T023 [US3] Add session validation and automatic redirect behavior for expired sessions in frontend/src/components/ProtectedRoute.jsx and frontend/src/services/authService.js
- [x] T024 [US3] Record admin login, logout, expiry, and unauthorized events in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AuditService.java

**Checkpoint**: At this point, User Story 3 should be fully functional and independently verifiable.

---

## Phase 6: User Story 4 - Scalable role-based access model for future customer features (Priority: P3)

**Goal**: Ensure the access model supports future roles and customer-facing features without redesigning the login foundation.

**Independent Test**: Add a new admin or future role mapping and confirm the access model works without breaking the current sign-in and authorization flow.

### Implementation for User Story 4

- [x] T025 [P] [US4] Extend the role model to support future role names and admin checks in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/Role.java
- [x] T026 [US4] Implement the role-permission mapping for bank teller and bank manager with shared access in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AuthorizationService.java
- [x] T027 [US4] Update the frontend navigation visibility to reflect role-based access patterns in frontend/src/components/Sidebar.jsx and frontend/src/components/Header.jsx
- [x] T028 [US4] Seed the database-backed test admin users for bank teller and bank manager in backend/src/main/resources/data.sql
- [x] T029 [US4] Update the validation guide to describe the test-admin role model and expected login behavior in specs/001-admin-login/quickstart.md

**Checkpoint**: All user stories should now be independently functional.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final review for security, consistency, and release readiness.

- [x] T030 [P] Review and harden access-control messages and account state handling across backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller and frontend/src/pages/auth
- [x] T031 Run the full admin login validation flow from specs/001-admin-login/quickstart.md and confirm all expected outcomes are met
- [x] T032 [P] Perform a final pass on session, role, and audit logging behavior in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service and backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies.
- **Phase 2 (Foundational)**: Depends on Phase 1 completion and blocks all user stories.
- **Phase 3+ (User Stories)**: All depend on Phase 2 completion.
- **Phase 7 (Polish)**: Depends on all desired user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational and does not depend on other stories.
- **User Story 2 (P2)**: Can start after Foundational and depends on the auth foundation built in User Story 1.
- **User Story 3 (P3)**: Can start after Foundational and depends on the session model established in User Story 1.
- **User Story 4 (P3)**: Can start after Foundational and depends on the role and access model introduced by User Story 2.

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel.
- All Foundational tasks marked [P] can run in parallel.
- User Story 1 can be implemented independently after the foundation is complete.
- User Story 2 can proceed in parallel with User Story 3 and User Story 4 after the identity and auth structures are ready.
- Final Polish tasks can run in parallel once all user stories are complete.

---

## Parallel Example: User Story 1

```bash
# Launch the core login tasks in parallel after Foundation:
Task: "Create database-backed test admin seed records in backend/src/main/resources/data.sql"
Task: "Build the admin sign-in page UI in frontend/src/pages/auth/Login.jsx"
Task: "Implement login request processing in backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/AuthService.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup.
2. Complete Phase 2: Foundational.
3. Complete Phase 3: User Story 1.
4. Validate the admin login flow with a database-backed test admin account.
5. Move to User Story 2 and beyond only after the sign-in flow is working.

### Incremental Delivery

1. Deliver a working admin sign-in flow for bank tellers and bank managers.
2. Add protected route enforcement and role checks.
3. Add session expiry, logout, and audit logging.
4. Finalize the role-based access model so future roles and customer-facing features can fit without redesigning the login foundation.
