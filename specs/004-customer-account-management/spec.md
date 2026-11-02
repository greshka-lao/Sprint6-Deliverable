# Feature Specification: Customer Account Consolidation

**Feature Branch**: `004-customer-account-management`

**Created**: 2026-10-30

**Status**: Draft

**Input**: User description: "Merge the "customer" table in the database to "customer_accounts" that you created. Name the table "customer". The join connections to "person" and "company" from the original "customer" table should retain. This table should centralise the data of customers for customer log-in and the admin's ability to create, update and delete customers. For the customer registration, there should be a form for selecting the customer type - person or company - and should be entered when creating a new customer. Same rules as the admin's customer editing applies - the admin cannot edit the customer's customer type. For the /admin/customer page, only customers with the ACTIVE status and the following details for these should appear: ID, name, username, address (concatenated like before), customer type, phone, email."

## Clarifications

### Session 2026-10-30

- Q: How should the migration match an existing person/company customer profile to a customer login account when there is no existing link? → A: Match only exact, unique name-and-complete-address pairs; preserve unmatched or ambiguous source records and flag them for manual resolution.
- Q: When an administrator deletes a customer, should the linked person or company profile also be deleted? → A: Remove customer login access but preserve the joined person/company subtype; because these share a primary key, mark the customer `DELETED` and clear credentials rather than physically deleting the inheritance root.
- Q: How should an administrator-created customer get customer sign-in credentials? → A: The customer requests a profile claim, an administrator approves it, and the customer completes registration to set their own password.
- Q: What should prove that a person claiming an administrator-created profile is authorized to use it, beyond matching its name and address? → A: Require administrator review and approval before attaching login credentials to the profile.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Register and sign in with a customer type (Priority: P1)

A new customer registers by choosing whether the account represents a person or a company. The customer can then sign in using the created account, and the selected customer type remains associated with the correct person or company record.

**Why this priority**: Registration and sign-in depend on one consistent customer account and profile, and the customer type determines which existing customer record the account represents.

**Independent Test**: Register one person customer and one company customer, verify that each has the selected type and corresponding association, then sign in using each account.

**Acceptance Scenarios**:

1. **Given** a prospective customer is completing registration, **When** they choose Person or Company and submit valid details, **Then** one customer account is created with that type and its corresponding person or company association.
2. **Given** a customer omits the customer type or submits a value other than Person or Company, **When** they submit registration, **Then** the account is not created and they receive clear guidance to correct the selection.
3. **Given** a customer account is successfully registered, **When** the customer signs in with its credentials, **Then** the customer can access the customer experience.
4. **Given** a customer account existed before the consolidation, **When** the consolidated account is used to sign in, **Then** its existing login remains usable and its person or company association is retained.
5. **Given** an administrator-created profile has no login credentials, **When** a customer requests a claim using an exact, unique name-and-complete-address match, **Then** the request awaits administrator approval and does not grant sign-in access.
6. **Given** an administrator approves a pending claim, **When** the customer completes registration, **Then** their login credentials are attached to the existing profile and no duplicate customer is created.
7. **Given** an administrator rejects a pending claim, **When** the customer attempts to sign in or complete registration for that profile, **Then** no login access is granted and the profile remains unchanged.

### User Story 2 - Manage customer accounts (Priority: P1)

An administrator creates, updates, and deletes customer accounts from the admin experience. The administrator chooses the customer type when creating a customer, but cannot change that type after creation.

**Why this priority**: Administrators need to manage customers in the same central account record used for customer sign-in, without allowing an edit to change the underlying person or company identity.

**Independent Test**: As an administrator, create a customer of each supported type, update editable customer details, attempt to change the type, and delete a customer after confirming the action.

**Acceptance Scenarios**:

1. **Given** an administrator is creating a customer, **When** they select Person or Company and submit valid details, **Then** the customer is created in the central customer account record with the selected type.
2. **Given** an administrator is editing an existing customer, **When** they change editable details and save, **Then** those details are updated while the customer type remains unchanged.
3. **Given** an administrator submits an update that attempts to change the customer type, **When** the update is processed, **Then** the customer's existing type and corresponding person or company association remain unchanged.
4. **Given** an administrator chooses to delete a customer, **When** they confirm deletion, **Then** customer credentials are cleared, the customer is marked `DELETED`, its joined person/company subtype remains, and bank accounts are unassigned; when they cancel, **Then** the customer remains unchanged.
5. **Given** a customer is signed in, **When** they attempt an administrator-only customer-management action, **Then** the action is denied.

### User Story 3 - Review active customers (Priority: P1)

An administrator uses `/admin/customer` to review active customers and their key account, contact, and address details in one place.

**Why this priority**: The admin list is the primary way to find and verify centrally managed customer accounts.

**Independent Test**: Create active and non-active customer records with known values, open `/admin/customer`, and compare the displayed rows and fields to those records.

**Acceptance Scenarios**:

1. **Given** active and non-active customer records exist, **When** an administrator opens `/admin/customer`, **Then** only customers whose status is ACTIVE are listed.
2. **Given** an active customer appears in the list, **When** the administrator reviews its row, **Then** the row shows ID, name, username, concatenated address, customer type, phone, and email.
3. **Given** an active customer has address components, **When** its address is displayed, **Then** the address uses the existing format: street number, city, province, postal code.
4. **Given** a customer is updated or deleted, **When** the administrator next views the list, **Then** the displayed customer data reflects that change and a deleted customer is absent.
5. **Given** a listed customer has a missing contact or address value, **When** the administrator views the row, **Then** the missing value is left blank and no other customer's value is shown.

### Edge Cases

- Existing customer and account records cannot be matched or contain conflicting person/company associations during consolidation; ambiguous and unmatched records require manual resolution without deleting their source data.
- A customer record has no person or company association matching its customer type.
- A customer attempts to claim an administrator-created profile with a non-exact or ambiguous name and address.
- A customer attempts to sign in or complete credential setup before an administrator approves their profile claim.
- A customer registration or admin creation request omits a required customer type or provides an unsupported value.
- A customer has a status other than ACTIVE; it must not appear in the admin customer list.
- A customer has missing contact or address information; the list must not show another customer's details in its place.
- An administrator attempts to change the customer type through an edit, regardless of how the update is submitted.
- An administrator cancels a customer deletion or attempts to delete a customer that no longer exists.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST maintain one central customer account record named `customer` for customer identity, login, and administrator-managed customer data.
- **FR-002**: The system MUST consolidate existing customer profile and customer account information into the central customer record without losing customer identity, login access, status, or person/company associations. Where no existing link exists, records MUST be matched only when the name and complete address match exactly and uniquely; unmatched or ambiguous source records MUST be preserved and flagged for manual resolution.
- **FR-003**: The system MUST retain the existing relationships between each customer and its associated person or company record. A customer's selected type MUST correspond to the retained association.
- **FR-004**: Customer registration MUST require an explicit choice of Person or Company and MUST save that choice as the customer's type.
- **FR-005**: The system MUST reject registration or customer creation when the type is missing or is not Person or Company, and MUST provide actionable validation feedback.
- **FR-006**: Successful customer registration MUST create a customer account that can be used to sign in under the existing customer authentication rules. If an exact, unique administrator-created profile with the same name and complete address exists without login credentials, the customer MUST request a claim and an administrator MUST approve it before the customer can complete registration and attach login credentials to that profile.
- **FR-007**: Administrators MUST be able to create customers in the central customer record and select their type during creation.
- **FR-008**: Administrators MUST be able to update customer details other than customer type. An administrator-submitted change to customer type MUST leave both the saved type and the corresponding person/company association unchanged.
- **FR-009**: Administrators MUST be able to delete a customer after explicit confirmation. Cancelling deletion MUST leave the customer and its login access unchanged; confirmed deletion MUST clear login credentials, mark the customer `DELETED`, and unassign its bank accounts while preserving its joined person/company subtype.
- **FR-010**: The `/admin/customer` page MUST list only customers whose status is ACTIVE.
- **FR-011**: Each customer row on `/admin/customer` MUST show, in order, ID, name, username, address, customer type, phone, and email.
- **FR-012**: The customer address on `/admin/customer` MUST be concatenated using the existing order and format: street number, city, province, postal code.
- **FR-013**: The displayed fields for a customer MUST come from that same customer's central record and associated profile; a missing value MUST be displayed as blank and MUST NOT be substituted with data belonging to another customer.
- **FR-014**: Customer account creation and administration MUST continue to respect the existing separation between customer access and administrator access.
- **FR-015**: Registration MUST NOT claim an administrator-created profile unless its name and complete address match exactly and uniquely and an administrator approves the claim. When no unique exact match exists or approval is denied, the system MUST NOT attach login credentials or alter the profile and MUST provide guidance to contact an administrator.
- **FR-016**: A pending profile claim MUST NOT grant sign-in access or attach login credentials before administrator approval; only the customer may set their password after approval.

### Key Entities *(include if feature involves data)*

- **Customer**: The central customer identity and login record, including its identifier, name, username, contact details, address, status, customer type, and login credentials.
- **Person**: The person profile associated with a customer whose type is Person.
- **Company**: The company profile associated with a customer whose type is Company.
- **Customer type**: The immutable-after-creation classification Person or Company that determines which profile is associated with a customer.
- **Unclaimed customer profile**: An administrator-created customer record without login credentials; a matching customer may request a claim, which requires administrator approval before the customer can set credentials through registration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of existing ACTIVE customer accounts in the consolidation verification set that have an existing link or an exact, unique name-and-complete-address match remain usable for customer sign-in after consolidation; all existing customer statuses and person/company associations remain intact, and unmatched or ambiguous source records are preserved for manual resolution.
- **SC-002**: 100% of valid registration and administrator-creation test cases save the selected Person or Company type; 100% of missing or unsupported-type cases are rejected without creating a customer.
- **SC-003**: 100% of customers shown on `/admin/customer` have ACTIVE status, and every displayed row contains the specified seven fields in the specified order with values belonging to that customer.
- **SC-004**: 100% of administrator edit attempts that include a changed customer type leave the customer's original type and person/company association unchanged.
- **SC-005**: At least 90% of first-time test customers can complete registration and reach a successful sign-in without administrator assistance.
- **SC-006**: 100% of confirmed customer deletions remove the customer login access while leaving the linked person or company profile intact.
- **SC-007**: 100% of administrator-created profile claims require administrator approval before login credentials are attached; unmatched, ambiguous, rejected, or pending claims grant no sign-in access and do not alter or duplicate the profile.

## Assumptions

- Existing customer registration and authentication rules remain in force except for requiring the customer-type selection and using the consolidated customer record.
- A newly registered customer is ACTIVE by default, consistent with the existing customer account behavior.
- The existing address display order is street number, city, province, postal code; no new address-formatting rules are introduced.
- Confirmed customer deletion removes login access by clearing credentials and marking the joined customer row `DELETED`; the person/company subtype row is retained.
- The existing admin authorization and customer authentication boundaries are unchanged.
