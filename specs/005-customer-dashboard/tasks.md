# Tasks: Customer Dashboard

**Input**: Design documents from `/specs/005-customer-dashboard/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/customer-dashboard.openapi.yaml`, `quickstart.md`

**Tests**: Existing customer authentication and dashboard UI tests are extended to verify the authenticated response, anonymous rejection, profile/account display, totals, and dashboard states.

**Organization**: Tasks are grouped by user story so the dashboard can be implemented and validated as an independent customer-facing increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with another task because it edits a different file and has no unmet task dependency.
- **[Story]**: Identifies the user story served by the task.
- Every implementation task names its exact file path.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm project/tooling initialization required by the implementation.

No setup tasks are required. The repository, Spring Boot backend, React frontend, dependencies, and test runners already exist.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared infrastructure before user-story implementation.

No foundational tasks are required. The customer session, customer-only security chain, protected `/customer/home` route, and customer request helper already exist.

---

## Phase 3: User Story 1 - Review a personal banking overview (Priority: P1) 🎯 MVP

**Goal**: After sign-in, show the authenticated customer's profile, all their bank accounts, and an accurate combined NZD total, without exposing another customer's data.

**Independent Test**: Sign in with a customer having known profile details and multiple linked accounts; verify the profile, every account, and their two-decimal total. Repeat with another customer and confirm only that customer's data appears; verify empty-account, missing-profile, unavailable-data, and unauthenticated states.

### Implementation

- [X] T001 [P] [US1] Define the customer dashboard response DTOs (profile, address, account, and total) in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerDashboardResponse.java`; keep nullable profile/address values, include account ID/type/balance/currency, and represent the total amount and currency.
- [X] T002 [P] [US1] Add a `getCustomerDashboard` request function in `frontend/src/services/customerAuthService.js` using the existing request helper and `/dashboard` path so the request includes the authenticated customer session cookie.
- [X] T003 [US1] Implement `CustomerDashboardService` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerDashboardService.java` to resolve the customer only by the authenticated principal's username, include only that customer's linked accounts through `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/AccountRepository.java`, preserve missing profile fields, label the platform currency NZD, sum balances using decimal arithmetic at two-decimal precision, and return a zero total for an empty account list; do not accept a customer ID from the caller.
- [X] T004 [P] [US1] Add `GET /api/customer-auth/dashboard` to `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthController.java`, use the authenticated principal and `CustomerDashboardService`, return the dashboard DTO, and verify authenticated profile/account data plus anonymous rejection in `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthSecurityTests.java`.
- [X] T005 [P] [US1] Replace the placeholder overview in `frontend/src/pages/customer/CustomerHome.jsx` with profile details, address, an account list, and a combined NZD total while retaining the customer header/sidebar; load through `getCustomerDashboard` and render explicit loading, request-error, missing-profile, and empty-account states without treating a failed request as an empty or zero-balance result; verify the UI states in `frontend/src/__test__/CustomerHome.test.jsx`.

**Checkpoint**: The signed-in customer's dashboard can be demonstrated independently at `/customer/home`; account ownership is derived from the server session, and all dashboard states are represented.

---

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Validate the finished dashboard and preserve existing authentication and admin behavior.

- [X] T006 [P] Run backend verification with `backend/mvnw.cmd -f backend/pom.xml test` and confirm the existing customer authentication/security and admin tests still pass.
- [X] T007 [P] Run frontend verification from `frontend/` with `npm test -- --run --maxWorkers=1` and `npm run build`; confirm the existing customer dashboard shell and admin UI remain functional.
- [X] T008 Verify the dashboard acceptance scenarios against `specs/005-customer-dashboard/quickstart.md` using `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthSecurityTests.java` and `frontend/src/__test__/CustomerHome.test.jsx`; record any remaining live-browser-only checks in the completion report.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No tasks; project initialization is already complete.
- **Foundational (Phase 2)**: No tasks; existing customer authentication and routing provide the required foundation.
- **User Story 1 (Phase 3)**: Can begin immediately.
- **Polish (Phase 4)**: Depends on the User Story 1 implementation being complete.

### User Story Dependencies

- **User Story 1 (P1)**: No dependencies on other user stories; it is the only story and forms the MVP.

### Within User Story 1

- T001 and T002 can start in parallel.
- T003 depends on T001's response contract.
- T004 depends on T001 and T003.
- T005 depends on T002 and can proceed in parallel with T003/T004 after T002 is complete.
- The final polish tasks require the completed dashboard.

### Dependency Graph

```text
T001 ──> T003 ──> T004 ──┐
                         ├──> T006, T007 ──> T008
T002 ───────────> T005 ──┘
```

## Parallel Execution Examples

### User Story 1

```text
Parallel: T001 — backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerDashboardResponse.java
          T002 — frontend/src/services/customerAuthService.js
After T001: T003 — backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerDashboardService.java
After T001 and T003: T004 — backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthController.java
After T002: T005 — frontend/src/pages/customer/CustomerHome.jsx (can run alongside T003/T004)
```

### Final Verification

```text
Parallel: T006 — backend/mvnw.cmd -f backend/pom.xml test
          T007 — frontend npm test and npm run build
After both pass: T008 — specs/005-customer-dashboard/quickstart.md manual scenarios
```

## Implementation Strategy

### MVP First

1. Reuse the existing customer authentication/session and route infrastructure; do not add setup or schema changes.
2. Implement the dashboard response and owner-scoped service.
3. Expose the protected dashboard read and replace the frontend placeholder.
4. Validate the P1 story end to end using the scenarios in `specs/005-customer-dashboard/quickstart.md`.

### Incremental Delivery

1. Complete T001–T005 to deliver the single independently useful user story.
2. Run backend and frontend regression checks in T006 and T007.
3. Complete the cross-customer, empty-account, missing-profile, and unavailable-data walkthrough in T008.

## Notes

- `[P]` tasks edit separate files and have no unmet dependencies.
- Every task uses the required checkbox, sequential ID, optional parallel marker, story label where required, and exact file path or runnable command.
- No test files are authored by this task list; the spec describes independent acceptance tests but does not request a TDD workflow or new automated test suites.
