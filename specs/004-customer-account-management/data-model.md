# Data Model: Customer Account Consolidation

## Customer

The central customer identity and administrative record, persisted in the final table named `customer`.

**Fields**
- `customerId`: generated primary identifier; authoritative identifier for customer and bank-account references after migration.
- `name`: required customer name.
- `customerType`: required immutable value `person` or `company`.
- `person` or `company`: the matching joined-inheritance subtype row, sharing the base `customer_id`; the other subtype row is absent.
- `address`: required association to the existing structured `Address`.
- `username`: unique generated username; absent while an administrator-created profile awaits a customer claim.
- `email`: normalized unique customer email; absent until credentials are attached.
- `phoneNumber`: E.164 phone number; absent until credentials are attached.
- `passwordHash`: adaptive one-way password hash; absent until credentials are attached.
- `role`: server-assigned customer role; never accepted from client input.
- `status`: customer state. Only `ACTIVE` customers appear in the admin list; sign-in additionally requires attached credentials.
- `createdAt`: record creation timestamp.
- `lastLoginAt`: nullable timestamp of the most recent successful sign-in.
- `bankAccounts`: existing one-to-many relationship to operational `Account` records.

**Validation and invariants**
- The customer type must be exactly `person` or `company`.
- Exactly one matching joined subtype is required: `Person` for type `person`, or `Company` for type `company`.
- Username, normalized email, and password hash are required before customer sign-in. They may be absent only for a newly created, unclaimed administrator profile.
- Generated usernames follow the existing customer-auth convention and remain unique. Email remains unique when present.
- Phone numbers are validated and stored in E.164 format when present.
- Customer type, customer role, and credentials are not changed through a customer profile edit. Credentials are attached only through approved claim completion.
- An administrator edit updates only permitted profile/contact fields and never changes type or person/company association.
- Confirmed customer deletion clears login credentials and unassigns bank accounts, then marks the central record `DELETED`. The joined `Person` or `Company` row remains because it shares the central customer's primary key.

## Person profile

The `Person` joined-inheritance subtype stored in the `person` table, with `customer_id` as both its primary key and foreign key to `customer.customer_id`.

**Fields**
- No additional person-specific attributes are present in the current `Person` entity; name and address remain on `Customer`.

**Relationships and lifecycle**
- A `Person` row is the subtype row for its central customer, and its `customer_id` is the same identifier.
- Customer removal is a soft deletion so the joined subtype row remains valid.
- Existing person identifiers are preserved during migration.

## Company profile

The `Company` joined-inheritance subtype stored in the `company` table, with `customer_id` as both its primary key and foreign key to `customer.customer_id`.

**Fields**
- No additional company-specific attributes are present in the current `Company` entity; name and address remain on `Customer`.

**Relationships and lifecycle**
- A `Company` row is the subtype row for its central customer, and its `customer_id` is the same identifier.
- Customer removal is a soft deletion so the joined subtype row remains valid.
- Existing company identifiers are preserved during migration.

## Address

The existing structured address, retained as a separate entity. `customer.fk_address_id` references `address.address_id` and is unique, so each customer has one address and an address cannot be shared by multiple customers. `Address` does not contain a customer foreign key.

**Fields**
- `addressId`
- `streetNumber`
- `postalCode`
- `city`
- `province`

The admin list renders the existing concatenation order: street number, city, province, postal code.

## CustomerClaimRequest

A short-lived workflow record for an unauthenticated customer requesting login access to an existing administrator-created profile.

**Fields**
- `claimRequestId`: generated request identifier.
- `customer`: the exact, unique unclaimed customer profile being requested.
- `claimReferenceHash`: hash of an unguessable, single-use reference held by the requesting browser; do not persist or log the raw reference.
- `submittedEmail`: validated email to attach after approval.
- `submittedPhoneNumber`: validated E.164 phone to attach after approval.
- `state`: `PENDING`, `APPROVED`, `REJECTED`, or `COMPLETED`.
- `submittedAt`: request timestamp.
- `reviewedAt`: nullable administrator decision timestamp.
- `reviewedBy`: nullable administrator identity that approved or rejected the request.
- `completedAt`: nullable timestamp when the customer finishes credential setup.

**Validation and transitions**
- Create a request only for an exact, unique match on name, complete address, and customer type to an unclaimed profile.
- Do not store a password or attach customer login credentials while the request is pending.
- `PENDING` transitions to `APPROVED` or `REJECTED` only through an authorized administrator action.
- `APPROVED` transitions to `COMPLETED` only when the claimant presents the valid reference and a valid password; email and username uniqueness are rechecked atomically.
- A rejected, completed, invalid, or consumed reference cannot grant credentials or mutate another customer.
- Do not reveal whether candidate customer profiles exist when a claim request cannot be uniquely matched.

## Legacy migration and relationships

- `Customer` remains the JPA root using `JOINED` inheritance with `Person` and `Company`. Both subtype tables retain `customer_id` as their shared primary/foreign key; no separate profile IDs or customer-to-profile link columns are introduced.
- Login fields are added to the central customer table. The migration adds `customer_type` and derives it from the existing joined subtype row without renaming profile IDs or rebuilding profile links.
- Copy existing login fields from `customer_accounts` into the matching central customer. Automatically match only exact, unique name and complete-address pairs; map each account to the legacy person/company type.
- Preserve all unmatched or ambiguous source rows and produce a manual-resolution report. Do not cut over authentication or retire source tables until every login account is linked or explicitly resolved.
- Update foreign keys from operational bank `Account` records to the authoritative central customer identifier, preserving existing customer/account relationships.
- Retain a migration mapping from prior customer/account IDs to the final `customerId` for verification and troubleshooting. Customer IDs returned after consolidation use the final central identifier; generated username and password hash are preserved for matched existing logins.
- Preserve a pre-migration database backup and source-table snapshot until post-cutover verification is accepted.
