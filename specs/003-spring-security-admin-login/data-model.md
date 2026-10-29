# Data Model: Spring Security for Admin Login

The design reuses administrator, role, session, and login-attempt records already present in the backend. It adds one narrowly scoped record for security events not represented by login attempts. Field names below describe logical data; the implementation may retain existing property names where compatible.

## Administrator Account

Represents an existing bank staff login identity.

| Field | Constraints and meaning |
|---|---|
| `id` | Stable, generated account identifier. |
| `username` | Required and unique login name. |
| `email` | Required and unique email address; usable as a login identifier. |
| `passwordHash` | Required protected credential. Existing unprefixed development credential is accepted only through the explicit legacy verifier and replaced by an adaptive encoded value after successful verification. Never return or log the value. |
| `role` | Required association to an administrator role. Only `BANK_TELLER` and `BANK_MANAGER` are eligible. |
| `enabled` | Administrative enablement state. A temporary failed-attempt lock must not set this value false. |
| `failedAttempts` | Consecutive failed attempts for the current lock period. Resets on successful sign-in and when a temporary lock expires. |
| `lockedUntil` | Nullable end time for a temporary five-failure lock. A value later than the current time blocks login; an elapsed value is cleared before a subsequent login is evaluated. |
| `createdAt` | Required account creation timestamp. |
| `lastLoginAt` | Nullable until the first successful login. |

### Account access states

- **Enabled**: `enabled=true`, with no unexpired temporary lock.
- **Temporarily locked**: `enabled=true`, `lockedUntil` is later than the current time. Lock duration is 15 minutes.
- **Administratively disabled**: `enabled=false`, independent of `lockedUntil`; this state is not automatically cleared when the temporary lock duration elapses.
- **Re-enabled**: An administrator restores `enabled=true`; any expired temporary lock and stale failed-attempt count are cleared before another attempt.

## Administrator Role

Represents the administrator authorization category.

| Field | Constraints and meaning |
|---|---|
| `id` | Stable role identifier. |
| `name` | Unique role name. `BANK_TELLER` and `BANK_MANAGER` are the accepted roles for this feature. |
| `description` | Human-readable purpose of the role. |

Both accepted roles receive the same existing customer and account management permissions.

## Administrator Session

Represents a revocable bearer-token session associated with one administrator account. It reuses the existing persisted `Session` record, but persists a one-way digest of the token rather than the raw bearer token.

| Field | Constraints and meaning |
|---|---|
| `id` | Stable, generated session identifier. |
| `tokenDigest` | Required unique one-way digest of a randomly generated high-entropy token. Raw token is returned only in the successful login response and is never stored, logged, or included in audit events. |
| `adminUser` | Required association to the owning administrator account. |
| `createdAt` | Required issuance timestamp. |
| `lastUsedAt` | Required timestamp updated after each successful session validation. |
| `expiresAt` | Required rolling inactivity deadline, set to 15 minutes after `lastUsedAt`. |
| `active` | False after logout, replacement by a new login, or administrator disablement. |

At most one active administrator session is retained per administrator, matching the existing behavior. A successful new login generates a fresh token and invalidates the previously active session. An inactive, unknown, disabled-owner, role-ineligible, or expired session cannot authenticate a request.

## Access Attempt

Represents a login attempt using the existing `AccessAttempt` record.

| Field | Constraints and meaning |
|---|---|
| `id` | Stable, generated attempt identifier. |
| `username` | Submitted login identifier; never contains a password or session token. |
| `success` | Whether authentication succeeded. |
| `createdAt` | Required attempt timestamp. |

Unknown-user and incorrect-password attempts are recorded in the same externally indistinguishable way. The fifth consecutive failure sets the 15-minute temporary lock.

## Administrator Security Event

Represents security-relevant events that are not login attempts.

| Field | Constraints and meaning |
|---|---|
| `id` | Stable, generated event identifier. |
| `adminUser` | Nullable relationship to an administrator; null when identity could not be established. |
| `eventType` | Required bounded event category, including sign-out, lockout, session expiry, and denied admin access. |
| `occurredAt` | Required event timestamp. |
| `outcome` | Required safe result category. |
| `subject` | Optional safe identifier such as username; must not contain credentials or bearer tokens. |

The event record must not contain raw or encoded passwords, bearer tokens, or request bodies. `AccessAttempt` remains the source for sign-in success/failure history; security events cover the additional audit events required by the specification.

## Relationships and lifecycle

- One `Role` may be assigned to many administrator accounts; each administrator account has one role.
- One administrator account may have historical sessions but at most one active session.
- One administrator account may have many access attempts and security events.
- Sign-in transitions from unauthenticated to authenticated only after password verification, any required credential upgrade, and successful session persistence.
- Logout, single-session replacement, expiry, account disablement, or role ineligibility makes the session unusable.
- Customer identity, customer role, and customer-session data have no relationship to administrator session or authorization data.
