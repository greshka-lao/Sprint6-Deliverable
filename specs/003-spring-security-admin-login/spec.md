# Feature Specification: Spring Security for Admin Login

**Feature Branch**: `003-spring-security-admin-login`

**Created**: 2026-10-29

**Status**: Draft

**Input**: User description: "Add Spring Security to the current backend for admin log-in. Please include feature overview, scope and non-goals, actors and preconditions, core behaviour and flows, business rules, error conditions and edge cases, non-functional requirements, API expectations."

## Feature Overview

This feature strengthens the existing administrator sign-in and access-control experience. Bank tellers and bank managers must be able to sign in, retain a valid session, and use authorized administrative functions while unauthenticated, expired, disabled, or locked users remain blocked. The change builds on the current admin identity and login experience and does not merge customer identities or sessions into the admin security boundary.

## Clarifications

### Session 2026-10-29

- Q: How should existing administrator credentials be transitioned to protected password storage? → A: Upgrade each account on its first successful sign-in; keep the old format only until that sign-in succeeds.
- Q: After five failed administrator sign-in attempts, should the account unlock automatically after 15 minutes or require an administrator to re-enable it? → A: The failed-attempt lock expires automatically after 15 minutes; disabled accounts still require administrator action.
- Q: Should the admin login keep returning its bearer token in the successful response, or should the API move to a secure session cookie? → A: Preserve the bearer token only in successful login responses; do not include it in error responses or audit records.

## Scope and Non-Goals

### In Scope

- Secure administrator sign-in for bank tellers and bank managers.
- Verification of credentials against existing administrator accounts.
- Protected access to existing administrative customer and account operations.
- Creation, validation, expiry, and invalidation of administrator sessions.
- Role checks that allow the existing administrator roles while rejecting customer or non-admin identities.
- Safe handling of existing administrator credentials and sessions during the transition.
- Consistent failures and audit records for sign-in, sign-out, session, and access decisions.
- Compatibility with the existing admin sign-in page and documented admin authentication API.

### Out of Scope

- Customer registration, customer sign-in, or changes to customer authentication behavior.
- New administrator provisioning, self-service password reset, or account recovery workflows.
- Changes to the permissions currently shared by bank tellers and bank managers.
- External identity providers, single sign-on, multi-factor authentication, or passwordless authentication.
- Changes to customer or account management business features unrelated to authorization.
- Redesign of the admin sign-in user interface.

## Actors and Preconditions

- **Unauthenticated visitor**: May access the admin sign-in experience and permitted public resources, but may not use protected admin operations.
- **Bank teller**: An active administrator who can access existing customer and account management operations.
- **Bank manager**: An active administrator who has the same access as a bank teller in this release.
- **Administrator account**: Must already exist, be enabled, and hold an accepted administrator role.
- **Application**: Must be available and connected to its configured administrator account store.
- **Existing customer-authentication boundary**: Customer identities and sessions remain independent and cannot satisfy administrator authorization.

## Core Behaviour and Flows

### Administrator sign-in

1. The administrator opens the existing admin sign-in page.
2. The administrator submits a username or email address and password.
3. The system verifies the credentials and confirms the account is active and eligible for administrator access.
4. On success, the system establishes an authenticated administrator session and returns the administrator to the admin area.
5. On failure, the system denies access, records the failed attempt, and presents an actionable message without disclosing whether a username or email exists.

### Protected operation

1. An administrator requests a protected customer or account operation.
2. The system validates the active session and the user's administrator role.
3. If both are valid, the operation proceeds.
4. If the session is missing, expired, or invalidated, the operation is denied and the user is required to sign in again.
5. If the identity is not an authorized administrator, the operation is denied without exposing protected data or performing a state change.

### Sign-out and expiry

1. An administrator chooses sign out, or the administrator session expires after inactivity.
2. The system invalidates the administrator session.
3. Subsequent protected requests are rejected and the user must sign in again.
4. Signing out of an administrator session does not alter a customer session, and customer sign-out does not alter an administrator session.

## Business Rules

- Bank tellers and bank managers are the only administrator roles in scope; both have the same current customer and account management permissions.
- Only an existing, enabled account with an accepted administrator role may establish an administrator session.
- The system must never treat a customer identity or session as an administrator identity or session.
- Administrator passwords must not be stored or logged in recoverable plaintext. On an existing account's first successful sign-in, the system verifies the old credential, replaces it with a protected credential, and completes sign-in only after the replacement is stored.
- Invalid username and invalid password attempts must produce equivalent public authentication errors.
- Five consecutive failed administrator sign-in attempts trigger a temporary 15-minute lockout. The failed-attempt lock expires automatically and resets its failure count; an account disabled by an administrator remains disabled until an administrator re-enables it.
- Administrator sessions expire after 15 minutes of inactivity. Explicit sign-out and administrator disablement invalidate access.
- The current admin authentication API's request and response contract remains compatible unless a future, explicitly approved change supersedes it.
- Security decisions must be enforced for backend operations as well as frontend navigation; hiding a page or link alone does not grant protection.

## Error Conditions and Edge Cases

- **Missing username or password**: Reject sign-in with a validation response; do not create a session.
- **Unknown username/email or incorrect password**: Return the same generic invalid-credentials response and record the failed attempt.
- **Locked or disabled account**: Deny sign-in and provide the existing account-status guidance without creating a session.
- **Temporary lockout expires**: After 15 minutes, allow another sign-in attempt for an account that is otherwise enabled; do not automatically re-enable an administratively disabled account.
- **Unknown or non-administrator role**: Deny administrator access even if the submitted credentials are otherwise valid.
- **Expired, revoked, malformed, or missing session credential**: Deny protected operations and require a new sign-in.
- **Customer session used for an admin operation**: Deny the operation and do not disclose or modify administrator data.
- **Sign-out after expiry or repeated sign-out**: Complete safely without restoring or affecting another session.
- **Credential conversion or account-store failure**: Deny sign-in; do not grant access using a plaintext or success-shaped fallback.
- **Credential upgrade cannot be stored**: Deny sign-in and retain no newly issued session; allow the administrator to retry after the account store is available.
- **Security service or account-store unavailable**: Fail closed for protected operations and communicate an appropriate service error.
- **Repeated failed attempts**: Apply the existing temporary lockout rule consistently without revealing account existence.
- **Concurrent sign-in or sign-out requests**: A completed sign-out or account disablement must not leave the affected administrator authorized for subsequent requests.

## API Expectations

The existing administrator authentication endpoints remain the contract boundary:

- **Sign-in**: `POST /api/auth/login` accepts the existing `username` and `password` fields; the username may identify an account by username or email address. A successful response retains the existing `token`, `username`, `role`, and `message` fields. Invalid input, invalid credentials, locked accounts, and service failures return appropriate non-success responses without exposing secrets.
- **Session validation**: `GET /api/auth/session` validates the current administrator session supplied in the existing `Authorization` header and returns the existing username, role, and valid-session response only when the session remains active and authorized.
- **Sign-out**: `POST /api/auth/logout` invalidates the current administrator session supplied in the existing `Authorization` header and returns the existing sign-out confirmation. Repeated sign-out must not restore access.
- **Protected operations**: Existing customer and account management operations require a valid administrator session. A missing, invalid, expired, or non-admin credential must not perform the requested operation.
- **Compatibility**: Existing admin clients continue to send and receive the documented authentication fields and use the existing endpoint paths. Raw passwords and internal authentication details are never returned.
- **Failure shape**: Error responses retain the existing safe error/message fields and identify an actionable category, such as validation, authentication failure, locked account, expired session, or access denied. Credential failures do not distinguish unknown accounts from incorrect passwords.

## Non-Functional Requirements

- **Security**: All protected administrator operations must enforce server-side authentication and role eligibility. Passwords and sensitive account data must not appear in logs or responses. The opaque bearer token is returned only by successful sign-in to preserve the existing admin API contract; it must never appear in error responses or audit records.
- **Reliability**: A failed security check must deny access; errors must not be converted into successful authentication or authorization.
- **Performance**: Under normal operating conditions, at least 95% of valid admin sign-ins must complete within five seconds.
- **Session handling**: Session inactivity, explicit sign-out, and account disablement must consistently remove access to protected operations.
- **Auditability**: Successful and failed sign-ins, sign-outs, lockouts, session expiry, and denied protected operations must be recorded with sufficient context for authorized operational review, without recording passwords or reusable session secrets.
- **Compatibility**: Existing admin sign-in and customer/account management workflows must continue to work for authorized admins, and customer authentication behavior must remain unchanged.
- **Usability**: Authentication and authorization errors must explain the next safe action without revealing credential validity or internal security configuration.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign in as an administrator (Priority: P1)

As a bank teller or bank manager, I want to sign in with my existing account so that I can securely access administrative customer and account workflows.

**Why this priority**: Sign-in is the required entry point to all protected administrator work.

**Independent Test**: Submit valid credentials for an active teller and manager account, verify each receives an authorized session and can reach the admin area, then submit invalid credentials and verify no session is created.

**Acceptance Scenarios**:

1. **Given** an enabled bank teller with valid credentials, **When** they sign in, **Then** the system establishes an admin session and grants access to the admin area.
2. **Given** an enabled bank manager with valid credentials, **When** they sign in, **Then** the system establishes an admin session with the same current administrative permissions as a teller.
3. **Given** a visitor submits an unknown username or an incorrect password, **When** sign-in is attempted, **Then** access is denied with the same generic credential error and a failed attempt is recorded.
4. **Given** a user submits missing credentials, **When** sign-in is attempted, **Then** the system returns a validation error and creates no session.

### User Story 2 - Protect administrator operations (Priority: P1)

As an administrator, I want protected operations to verify my active session and role so that only authorized staff can view or change customer and account records.

**Why this priority**: Authentication is ineffective unless backend operations independently enforce the access boundary.

**Independent Test**: Request a protected operation with a valid admin session, then repeat with no session, an expired session, and a customer session; verify only the valid admin request can proceed.

**Acceptance Scenarios**:

1. **Given** a valid teller or manager session, **When** the administrator requests a protected customer or account operation, **Then** the operation proceeds.
2. **Given** no session or an expired session, **When** a protected operation is requested, **Then** it is denied and no record is changed.
3. **Given** an authenticated customer session, **When** it is used for an admin operation, **Then** the system denies access and returns no protected data.
4. **Given** an account is disabled or has a non-admin role, **When** its holder attempts an admin operation, **Then** the system denies access.

### User Story 3 - End and expire administrator sessions (Priority: P2)

As an administrator, I want sign-out and inactivity expiry to invalidate my session so that a later request cannot reuse it.

**Why this priority**: Reliable session termination limits the risk of unattended or abandoned authenticated sessions.

**Independent Test**: Sign in, verify access, sign out, and verify subsequent protected requests fail; separately verify inactivity expiry produces the same reauthentication requirement.

**Acceptance Scenarios**:

1. **Given** an active administrator session, **When** the administrator signs out, **Then** the session is invalidated and protected requests require sign-in.
2. **Given** an administrator session has been inactive for 15 minutes, **When** the user requests a protected operation, **Then** access is denied and the user must sign in again.
3. **Given** an administrator signs out while a customer session exists, **When** the administrator session is invalidated, **Then** the customer session remains unchanged.

### Edge Cases

- Several failed attempts are submitted at the threshold for temporary lockout, or a retry occurs at the 15-minute expiry boundary.
- An administrator is disabled while a session is active.
- A session expires while the user is editing or submitting a protected operation.
- Sign-out is retried after the session has already expired.
- A customer-authenticated browser submits an admin request.
- The account store or authentication service is unavailable during login or session validation.
- Credential migration encounters an account with an unsupported or malformed stored credential.
- Multiple administrator sessions or simultaneous sign-in and sign-out requests occur for the same account.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST authenticate administrator sign-in using an existing username or email address and password.
- **FR-002**: The system MUST accept only enabled administrator accounts with the bank teller or bank manager role for administrator access.
- **FR-003**: The system MUST protect every existing customer and account management operation with a server-side administrator authentication and role check.
- **FR-004**: The system MUST maintain the existing admin sign-in, session-validation, and sign-out API paths and documented request and response fields.
- **FR-005**: The system MUST establish an administrator session only after successful credential validation and MUST reject missing, invalid, expired, or revoked sessions.
- **FR-006**: The system MUST invalidate an administrator session on explicit sign-out and after 15 minutes of inactivity.
- **FR-007**: The system MUST deny access to disabled, locked, or non-administrator accounts; after five consecutive failed sign-in attempts it MUST temporarily lock an otherwise enabled account for 15 minutes, automatically clear that temporary lock and its failure count at expiry, and leave administratively disabled accounts disabled until manually re-enabled.
- **FR-008**: The system MUST return equivalent public authentication failures for unknown usernames and incorrect passwords.
- **FR-009**: The system MUST protect stored administrator credentials from recovery in plaintext and upgrade an existing credential to protected storage during its first successful sign-in before establishing an authenticated session.
- **FR-010**: The system MUST keep administrator identity, authorization, and session state separate from customer identity and session state.
- **FR-011**: The system MUST record successful and failed sign-ins, sign-outs, lockouts, session expiries, and denied admin operations without recording passwords or reusable session secrets.
- **FR-012**: The system MUST fail closed when it cannot determine whether a requested administrator operation is authorized.
- **FR-013**: The system MUST preserve the existing admin sign-in experience and authorized admin workflows without changing customer authentication behavior.

### Key Entities

- **Administrator account**: An existing staff identity with a username, email address, protected password credential, enabled or locked status, and administrator role.
- **Administrator role**: The staff access category that determines administrator eligibility; bank teller and bank manager have the same permissions in this scope.
- **Administrator session**: A time-limited authorization state associated with one administrator account, with active, expired, or revoked status.
- **Security event**: A record of a sign-in, failed attempt, lockout, sign-out, expiry, or denied operation for operational review.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of unauthenticated, expired-session, disabled-account, and non-admin attempts to use protected admin operations are denied without performing the requested change.
- **SC-002**: At least 95% of valid administrator sign-ins complete within five seconds under normal operating conditions.
- **SC-003**: 100% of unknown-user and incorrect-password sign-in attempts return the same public error category and message.
- **SC-004**: 100% of valid administrator sign-outs and sessions inactive for 15 minutes result in denial of subsequent protected requests.
- **SC-005**: Existing authorized teller and manager workflows and the documented admin authentication API pass their regression checks with no customer-authentication regressions.
- **SC-006**: No raw password appears in any response or security audit record; the bearer token appears only in a successful admin login response and never in an error response or audit record.

## Assumptions

- Existing administrator accounts, roles, and authentication endpoint paths remain the source of truth for this change.
- The 15-minute inactivity timeout remains in force. Failed-attempt lockout is separate: five consecutive failures cause a 15-minute automatic lock, while administratively disabled accounts require manual re-enablement.
- Bank teller and bank manager continue to share permissions for current admin workflows.
- The existing admin frontend remains the sign-in client; this feature does not redesign its UI.
- Existing customer authentication already has a separate security boundary and must continue to work independently.
- Existing administrator credentials may use a legacy format; they are upgraded only after a successful credential check, and sign-in is not completed unless the protected credential is stored successfully.
- API contract details are included because the requested feature explicitly asks for API expectations; implementation technology and class-level design are intentionally left to planning.
