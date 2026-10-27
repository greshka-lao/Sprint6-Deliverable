# Data Model: Admin Login

## Core Entities

### AdminUser
Represents an application user who can access the administrative area.

**Fields**
- id: unique identifier
- username: unique login identifier
- email: unique contact address used for login or recovery
- passwordHash: secure representation of the password
- roleId: reference to the user's role
- status: active, inactive, locked, disabled
- createdAt: record creation timestamp
- updatedAt: last metadata update timestamp
- lastLoginAt: most recent successful sign-in timestamp

**Validation rules**
- Username and email must be unique within the system.
- Password must be stored as a hashed value, never in plain text.
- Account status must be active before sign-in is permitted.
- Users without an assigned admin role cannot access protected features.

### Role
Represents a permission grouping for access control.

**Fields**
- id: unique identifier
- name: role name (for example, bank teller or bank manager)
- description: human-readable summary of the role
- isAdmin: boolean flag indicating if the role is allowed into the admin area
- createdAt: creation timestamp

**Validation rules**
- Role names must be unique.
- Only admin roles may access the admin dashboard and protected actions.

### Session
Represents a valid authenticated admin session.

**Fields**
- id: unique identifier
- userId: reference to the authenticated admin user
- token: secure session identifier or equivalent session token
- issuedAt: session creation timestamp
- expiresAt: session expiry timestamp
- lastActivityAt: last recorded request activity
- revokedAt: optional timestamp when session ended manually

**Validation rules**
- Sessions must be valid for only the authenticated user.
- Sessions expire after inactivity or after the configured maximum session lifetime.
- Expired or revoked sessions must deny access to protected routes.

### AccessAttempt
Represents a sign-in request and the resulting authorization outcome.

**Fields**
- id: unique identifier
- userId: reference to the user if a known username or email matches
- usernameOrEmail: submitted credential value
- result: validation_error, authentication_failed, account_locked, session_expired, access_denied, success
- attemptedAt: timestamp of the attempt
- ipAddress: optional operational information for monitoring and audit review
- notes: optional summary of the failure or decision

**Validation rules**
- Failed authentication attempts must be logged even when no account match is found.
- Successful attempts must be linked to an active user session.
- Lockout and access-denied decisions should be recorded as part of the audit trail.

## Relationships

- One role can be assigned to many admin users.
- One admin user can have one active session at a time in this feature scope.
- One admin user can have many access attempts over time.
- A session belongs to a single user.

## State Model

### Admin account lifecycle
- Created → Active → Locked/Disabled → Inactive
- Active accounts may sign in.
- Locked or disabled accounts cannot sign in.

### Session lifecycle
- New → Active → Expired
- Active → Revoked by logout
- Expired or revoked sessions require reauthentication.

## Business constraints

- The initial release supports two admin roles: bank teller and bank manager.
- Both roles initially share the same permission set for customer and account management actions.
- The model is designed to support future roles and permissions without redesigning the sign-in foundation.
