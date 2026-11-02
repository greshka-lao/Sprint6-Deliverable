# Research: Customer Account Consolidation

## Decision 1: Promote customer login identity into the central `customer` entity

**Decision**: Move the login fields currently stored in `CustomerAccount` into the central `Customer` record, and query that entity for customer authentication. Keep the final root table named `customer`.

**Rationale**: The requested outcome is one customer record for login, administration, and profile association. Today, `Customer` and `CustomerAccount` are separate JPA entities, repositories, and MySQL tables; leaving both as active sources would continue the split the feature is meant to remove.

**Alternatives considered**:
- Keep `customer_accounts` as the login store and join it to `customer` at read time: rejected because customer identity remains split across two records and admin CRUD can drift from login data.
- Rename the auth table and discard the operational customer hierarchy: rejected because person/company and bank-account associations must remain.

## Decision 2: Retain joined inheritance for person and company subtypes

**Decision**: Keep `Customer` as the central entity with JPA `JOINED` inheritance. `Person` and `Company` remain subclasses whose `customer_id` is both the subtype primary key and a foreign key to `customer.customer_id`; the matching subtype determines the immutable customer type.

**Rationale**: This preserves the existing database relationship and the requested `customer_id` column on both subtype tables while keeping customer login data on the central row. Since deleting a joined-inheritance root also deletes its subtype, customer deletion clears credentials and marks the central row `DELETED` instead of physically deleting it; bank accounts are unassigned.

**Alternatives considered**:
- Rename subtype identifiers and use independent profile associations: rejected because it changes the existing `customer_id` structure the user wants to retain.
- Physically delete a joined-inheritance root: rejected because the subtype profile row would also be deleted.

## Decision 3: Require an administrator-reviewed claim before attaching credentials to an admin-created profile

**Decision**: A customer submits a claim request only for an exact, unique name-and-complete-address match. The request remains pending without credentials; an authorized administrator approves or rejects it. Only after approval can the customer complete registration and attach a password, username, email, and phone to the existing record. Use an unguessable, single-use claim reference so the registration browser can check status and complete the approved claim.

**Rationale**: Names and addresses are not proof of identity. Administrator approval prevents a public registration request from taking over another customer's profile. Customer-set passwords avoid administrator handling of password secrets, and the reference ties the approved claim to the submitting browser without requiring a new email provider.

**Alternatives considered**:
- Automatically claim on exact name/address alone: rejected because public profile information could enable account takeover.
- Have an administrator create or communicate an initial password: rejected because it exposes password handling to staff.
- Add email delivery and verification: rejected because the project has no email integration and it is not required by the specification.

## Decision 4: Perform a staged, operator-controlled data migration

**Decision**: Add versioned, documented MySQL preflight and migration SQL under `backend/src/main/resources/db/manual/`. Back up the schema first, report exact unique matches and unresolved records, stop cutover if any login account remains unresolved, validate associations and login data, then retire the source `customer_accounts` table only after acceptance. Do not rely on Hibernate `ddl-auto=update` to move data. Use schema validation after the migration has run.

**Rationale**: The project has no Flyway/Liquibase migration framework; current properties use Hibernate `update` in the default/development profile and `validate` in production. Hibernate schema update cannot safely reconcile overlapping identifiers, link profile subclasses, or guarantee that account data is not lost. A controlled SQL process fits the current dependency set while providing a reviewable cutover.

**Alternatives considered**:
- Let Hibernate create/update the merged schema: rejected because it does not migrate values or resolve identity collisions safely.
- Add a migration framework for this single conversion: rejected for this feature because it adds dependencies and deployment bootstrapping to an existing populated schema; revisit if the project adopts ongoing versioned migrations.
- Drop unresolved legacy rows and proceed: rejected because customer sign-in and data preservation are acceptance requirements.

## Decision 5: Preserve existing authorization, test tooling, and UI conventions

**Decision**: Continue using Spring Security's existing customer and administrator filter chains, the existing `/customer/**` admin API authorization, Maven backend tests, and Vitest/Testing Library frontend tests. Add claim approval to the existing administrator boundary and customer-facing claim status/completion to the customer-auth boundary.

**Rationale**: The project already has separated customer and admin authentication, CSRF for customer-auth state changes, bearer-token admin authorization, and established frontend API/test patterns. Reusing these boundaries minimizes security drift.

**Alternatives considered**:
- Create a third authentication mechanism or external identity service: rejected as unnecessary for this feature.
- Treat frontend route guards as authorization: rejected because all admin claim operations must be protected by the backend.

## Decision 6: Retain current product-scale performance expectations

**Decision**: Do not introduce a numeric throughput or availability target. Validate the admin listing and customer flows with the existing application/test data and ensure the active-status filter and username lookup use indexed/unique fields.

**Rationale**: The specification provides no user volume or deployment SLO. A numeric target would be speculative; correctness and responsiveness under normal current use are measurable for this feature.

**Alternatives considered**:
- Invent a new latency, concurrency, or uptime objective: rejected because there is no project baseline or stated scale.
