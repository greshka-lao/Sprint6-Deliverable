# Tasks: Customer Account Consolidation

**Input**: Design documents from `specs/004-customer-account-management/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/customer-management-api.md`, and `quickstart.md`

**Tests**: Test tasks are included for the independent user-story acceptance scenarios and migration safety requirements in the feature specification.

**Organization**: Tasks are grouped by user story. Shared schema and migration work is foundational because all three stories depend on a single central customer identity.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: The task can proceed in parallel with other marked tasks because it changes a different file and has no dependency on unfinished work.
- **[Story]**: User-story label corresponding to the feature specification.
- Every task names its target file path.

## Phase 1: Setup

**Purpose**: Prepare a reproducible legacy-schema fixture and align the validation guide with the feature's staged migration.

- [X] T001 [P] Add a sanitized legacy-schema fixture containing person, company, customer, customer_accounts, address, and bank-account relationships plus unique, unmatched, and ambiguous match cases in `backend/src/test/resources/db/customer-consolidation-legacy.sql`.
- [X] T002 [P] Document backup, fixture setup, preflight review, and cutover prerequisites in `specs/004-customer-account-management/quickstart.md`.

## Phase 2: Foundational

**Purpose**: Establish the single customer entity, preserve profile/bank relationships, and provide a safe migration before story work begins.

- [X] T003 Keep `Customer` as the central joined-inheritance root in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/Customer.java`; enforce immutable `customerType` matching the subtype, and `username`, normalized unique `email`, and `passwordHash` absent only for a new unclaimed admin profile; require credentials before sign-in and validate optional phone numbers as E.164.
- [X] T004 Keep `Person` and `Company` as joined subclasses in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/Person.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/Company.java`; retain the shared `customer_id` primary/foreign key in each subtype table and soft-delete customer logins so subtype rows remain.
- [X] T005 Update `Account.customer` mapping and foreign-key ownership to reference the central customer identifier without losing existing bank-account assignments in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/Account.java`.
- [X] T006 Consolidate repository access on `CustomerRepository`, adding unique username/email and active-list queries, and remove active authentication use of `CustomerAccountRepository` and `CustomerAccount` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/CustomerRepository.java`, `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/CustomerAccountRepository.java`, and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerAccount.java`.
- [X] T007 Create a read-only migration preflight that reports duplicate exact matches, unmatched/ambiguous account rows, conflicting person/company types, duplicate usernames/emails, and invalid bank-account references without changing source data in `backend/src/main/resources/db/manual/customer-consolidation-preflight.sql`.
- [X] T008 Create the staged migration that transfers login fields and password hashes, preserves statuses and joined subtype rows, retains `customer_id` as each subtype key, and preserves bank-account foreign keys and an old-to-central ID mapping in `backend/src/main/resources/db/manual/customer-consolidation.sql`; automatically match only exact, unique name-and-complete-address pairs and abort cutover while any login row remains unresolved.
- [X] T009 Set Hibernate schema generation to validation after the explicit data migration has run in `backend/src/main/resources/application.properties`, `backend/src/main/resources/application-development.properties`, and `backend/src/main/resources/application-production.properties`.

**Checkpoint**: The application model represents one central customer and the existing schema can be preflighted and migrated without discarding unresolved source records.

## Phase 3: User Story 1 - Register and sign in with a customer type (Priority: P1) 🎯 MVP

**Goal**: A customer can register as a person or company and sign in; claims of admin-created profiles never grant access before administrator approval.

**Independent Test**: Register one Person and one Company directly, verify the selected type and profile link, and sign in to each account. Submit a valid claim for an unclaimed profile and verify it cannot sign in before approval.

### Tests for User Story 1

- [X] T010 [P] [US1] Add backend tests for missing/unsupported customer type, Person/Company registration, username/email uniqueness, and sign-in against the central customer in `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Service/CustomerAuthServiceTests.java`.
- [X] T011 [P] [US1] Add registration UI tests for required Person/Company selection, validation errors, direct registration, and the claim-required response in `frontend/src/__test__/CustomerRegistration.test.jsx`.
- [X] T012 [P] [US1] Add claim service/controller security tests proving pending, rejected, invalid, and consumed claim references cannot create credentials or authenticate in `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerClaimSecurityTests.java`.

### Implementation for User Story 1

- [X] T013 [US1] Add required `customerType` to the registration request and validate name, address, email, E.164 phone, and password while preserving existing authentication validation rules in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerRegistrationRequest.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerAuthService.java`.
- [X] T014 [US1] Refactor registration, username allocation, and sign-in to persist/query the central `Customer`; create the matching person/company profile for direct registrations, preserve matched legacy password hashes, and allow sign-in only for ACTIVE customers with completed credentials in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerAuthService.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerUsernameService.java`.
- [X] T015 [US1] Create `CustomerClaimRequest` and repository with `PENDING`, `APPROVED`, `REJECTED`, and `COMPLETED` states; persist only a hash of the unguessable single-use claim reference, never a password, in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/CustomerClaimRequest.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/CustomerClaimRequestRepository.java`.
- [X] T016 [US1] Implement exact unique name, complete-address, and matching-type claim requests; expose claim submission, reference-status, and approved completion endpoints; ensure credentials are attached only after approval and reference consumption, with no raw reference in URLs or logs, in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerClaimService.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerAuthController.java`.
- [X] T017 [US1] Add the required Person/Company selector and direct-registration versus pending-claim states to `frontend/src/pages/auth/CustomerRegistration.jsx`.
- [X] T018 [US1] Add claim submission, status polling, and approved credential completion calls using the claim reference in a header rather than a URL in `frontend/src/services/customerAuthService.js`.

**Checkpoint**: New person and company registrations can sign in; an unapproved profile claim never grants access.

## Phase 4: User Story 2 - Manage customer accounts (Priority: P1)

**Goal**: Administrators create, update, and delete centrally stored customers without changing type, and review profile claims before credentials are attached.

**Independent Test**: As an administrator, create both types, edit permitted fields, attempt a type change, approve and reject claims, and delete a customer while verifying login removal and profile preservation.

### Tests for User Story 2

- [X] T019 [P] [US2] Add backend tests for admin customer create/update/delete, type immutability, profile preservation, and existing administrator authorization in `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerManagementSecurityTests.java`.
- [X] T020 [P] [US2] Add backend tests for approving/rejecting only pending claims and for ensuring claim decisions do not set passwords or alter profiles in `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Service/CustomerClaimServiceTests.java`.
- [X] T021 [P] [US2] Add frontend tests for required customer type on create and non-editable type on update in `frontend/src/__test__/AddCustomer.test.jsx` and `frontend/src/__test__/UpdateCustomer.test.jsx`.

### Implementation for User Story 2

- [X] T022 [US2] Implement administrator customer create, detail, update, and soft-delete against the central `Customer`; require a supported type on creation, ignore or reject update attempts to change type/subtype, and preserve the joined person/company row on deletion by clearing credentials and marking the customer `DELETED` in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerService.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerController.java`.
- [X] T023 [US2] Require a selected customer type in the admin create form and show the existing type as read-only during editing in `frontend/src/pages/admin/customer/AddCustomer.jsx` and `frontend/src/pages/admin/customer/UpdateCustomer.jsx`.
- [X] T024 [US2] Add administrator-only pending-claim listing, approve, and reject operations using the existing admin bearer-token authorization; allow approval to change only claim state and reviewer metadata in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Service/CustomerClaimService.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerClaimAdminController.java`.
- [X] T025 [US2] Add the admin claim-review page and route, displaying enough request/profile information for review without exposing passwords or raw claim references in `frontend/src/pages/admin/customer/CustomerClaims.jsx`, `frontend/src/App.jsx`, and `frontend/src/components/Sidebar.jsx`.
- [X] T026 [US2] Connect the claim-review UI to administrator claim list/approve/reject requests using the existing admin authorization conventions in `frontend/src/services/customerService.js` and `frontend/src/pages/admin/customer/CustomerClaims.jsx`.

**Checkpoint**: Admin CRUD and claim decisions use the central customer record; type changes are rejected or ignored, and deletion preserves the profile.

## Phase 5: User Story 3 - Review active customers (Priority: P1)

**Goal**: `/admin/customer` lists active customers and the seven specified data fields in the required order.

**Independent Test**: Seed active and inactive customers, view `/admin/customer`, verify only active rows appear and all seven fields belong to the same customer in the specified order.

### Tests for User Story 3

- [X] T027 [P] [US3] Add backend tests for active-only results, correct customer-specific contact values, and missing values in `backend/src/test/java/com/fdmgroup/sprint4Deliverable/Service/CustomerManagementTests.java`.
- [X] T028 [P] [US3] Add frontend tests for active list rows, field order, address concatenation, missing values, and updated/deleted records in `frontend/src/__test__/Customers.test.jsx`.

### Implementation for User Story 3

- [X] T029 [US3] Return active central customers with ID, name, username, address, type, phone, and email from `GET /customer/getAll`, using the address order street number, city, province, postal code and blank/null values for missing data in `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Repository/CustomerRepository.java` and `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Controller/CustomerController.java`.
- [X] T030 [US3] Render only the seven requested columns in the required order at `/admin/customer`, format the address consistently, and retain a redirect from the existing plural path if applicable in `frontend/src/pages/admin/customer/Customers.jsx` and `frontend/src/App.jsx`.
- [X] T031 [US3] Load the active-customer list through the central customer API and refresh it after admin updates/deletions without displaying values from another customer in `frontend/src/services/customerService.js` and `frontend/src/pages/admin/customer/Customers.jsx`.

**Checkpoint**: Active-only customer list meets the specified output and formatting.

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Keep prior docs consistent and verify migration, integration, and acceptance scenarios.

- [X] T032 [P] Update the earlier customer-auth API contract to replace the separate `customer_accounts` store and no-operational-customer-link assumptions with the consolidated customer and approved-claim behavior in `specs/002-customer-auth/contracts/customer-authentication-api.md`.
- [X] T033 [P] Update the feature API contract with the implemented request/response fields, claim errors, administrator endpoints, and auth boundaries in `specs/004-customer-account-management/contracts/customer-management-api.md`.
- [ ] T034 Run the migration fixture/preflight/cutover verification steps and the full customer scenario in `specs/004-customer-account-management/quickstart.md`; record any schema/data mismatch and do not cut over while source login rows remain unresolved.
- [X] T035 Run backend Maven tests and frontend Vitest/build checks for the customer feature from `backend/` and `frontend/`, and resolve failures in the corresponding test or implementation files.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; the legacy fixture and migration guide updates can proceed in parallel.
- **Foundational (Phase 2)**: Depends on Setup. The central entity and relationships precede migration SQL; this phase blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on the central model, repositories, and migration mapping from Phase 2.
- **User Story 2 (Phase 4)**: Depends on Phase 2 and the `CustomerClaimRequest` lifecycle created in US1; the admin claim-decision operations complete US1's approved-claim flow.
- **User Story 3 (Phase 5)**: Depends on Phase 2. It can be implemented independently of US1 and US2 after the central customer repository exists.
- **Polish (Phase 6)**: Depends on all three user stories and the migration scripts being ready.

### User Story Dependencies

- **US1 (P1)**: Starts after the foundational phase. Direct registration/sign-in is independently testable. Claim completion requires US2's administrator approval operation.
- **US2 (P1)**: Starts after the foundational phase; claim review integrates with US1's request entity and claim lifecycle.
- **US3 (P1)**: Starts after the foundational phase and has no dependency on US1/US2 implementation beyond central customer data.

### Parallel Opportunities

- **Setup**: T001 and T002 can proceed in parallel.
- **US1 tests**: T010, T011, and T012 target separate backend/frontend test files and can proceed in parallel after Phase 2.
- **US2 tests**: T019, T020, and T021 can proceed in parallel after Phase 2; tests for claim review assume US1's claim entity contract.
- **US2 implementation**: T023 can proceed alongside T024 after T022's customer data rules are agreed; T025 can proceed in parallel with T024, while T026 depends on the page and API contract.
- **US3 tests**: T027 and T028 can proceed in parallel after Phase 2.
- **US3 implementation**: T030 can proceed independently from T029; T031 follows the central list contract.
- **Polish**: T032 and T033 can proceed in parallel; T034 and T035 run after implementation.

## Implementation Strategy

1. Complete setup and foundational entity/migration work first; preserve database backups and source tables throughout verification.
2. Deliver the MVP slice as direct Person/Company registration and sign-in (US1). Do not advertise profile-claim completion until US2 administrator review is implemented.
3. Implement admin CRUD and approved profile claims (US2), then the active-customer list (US3). US3 can be developed in parallel after the foundation when staffing permits.
4. Finish documentation, migration cutover rehearsal, and full test/build validation. Do not remove source tables or accept a cutover with unmatched/ambiguous login rows unresolved.
