# Quickstart: Customer Account Consolidation

## Prerequisites

- Java 17 and the repository's Maven wrapper.
- Node.js/npm and the frontend dependencies installed.
- A MySQL instance with a current backup and the existing application schema.
- Existing customer and account data must be available for migration preflight.
- Configure database connection values using the repository's existing property/environment conventions; do not put credentials in the SQL files or command history.

## Validate the migration before cutover

1. Take a restorable backup of the schema.
2. In one isolated test MySQL connection, load `backend/src/test/resources/db/customer-consolidation-legacy.sql` and then run `backend/src/test/resources/db/customer-consolidation-fixture-check.sql`. Expected results are match counts 1, 2, 0, and 1 for the unique, ambiguous, unmatched, and company cases; only bank account 9004 should be invalid. Do not run test fixtures against a production schema.
3. Run `backend/src/main/resources/db/manual/customer-consolidation-preflight.sql` against the target schema. It is read-only and reports login matches, profile conflicts, normalized username/email collisions, invalid bank-account references, and incomplete addresses.
4. Review the report for duplicate exact matches, unmatched customer accounts, conflicting person/company associations, duplicate usernames/emails, invalid bank-account references, shared customer address references, and incomplete addresses. Each customer must reference a distinct address through `customer.fk_address_id`.
5. Resolve all account rows flagged as unmatched or ambiguous before cutover. The migration must not disable or discard their existing login data.
6. Run `backend/src/main/resources/db/manual/customer-consolidation.sql` once in the controlled deployment window. It aborts before DDL when any login row is unresolved or ambiguous and retains `customer_accounts` as a rollback snapshot.
7. Verify migrated row counts, all person/company associations, all operational bank-account customer references, unique usernames/emails, and customer login before removing the source-table snapshot and migration map.

The SQL filenames describe the planned artifacts; implementation must provide executable scripts and document their repeatable preflight/cutover usage. Do not use Hibernate `ddl-auto=update` as a substitute for the data migration.

## Run automated tests

From `backend/`:

```powershell
.\mvnw.cmd test
```

From `frontend/`:

```powershell
npm test -- --run
npm run build
```

Expected result: backend tests pass for migration matching, profile associations, customer-type immutability, claim authorization and lifecycle, customer authentication, and active-only customer listing. Frontend tests pass for registration type selection, pending/approved/rejected claim behavior, administrator claim review, customer CRUD, and the requested list columns/order.

## Start the application

Configure the existing backend database settings, then run from `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

In another terminal, run from `frontend/`:

```powershell
npm run dev
```

Expected result: the frontend loads against the backend, administrators can manage customer profiles and review claims, and customers can register, complete approved claims, and sign in.

## End-to-end scenarios

1. Sign in as an authorized administrator and create one Person and one Company customer profile. Confirm the type is saved and cannot be changed in edit.
2. Register a customer who has no matching admin-created profile. Select the type, complete the required details, and verify that the generated username can sign in.
3. Request a claim for a unique exact name/address/type match to an unclaimed profile. Confirm it cannot sign in while pending.
4. Approve the claim as an administrator. Complete credential setup using the claim reference and verify the customer can sign in to the existing profile without a duplicate row.
5. Reject another claim and confirm no credentials are attached and no profile data changes.
6. Submit an attempted type change through the admin update request and confirm the stored type and profile association remain unchanged.
7. Create or retain an inactive customer and verify `/admin/customer` excludes it. Verify active list rows show ID, name, username, address, customer type, phone, and email in order.
8. Delete a customer after confirmation. Verify credentials are cleared, the customer is marked `DELETED`, login is denied, the joined person/company subtype row remains, and assigned bank accounts are unassigned.
9. Verify migrated existing accounts can sign in and their person/company and bank-account associations remain intact. Do not accept cutover with unresolved migration records.
