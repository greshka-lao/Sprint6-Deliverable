# Administrator Authentication API Contract

The existing API is hosted under the configured servlet context path `/sprint4Deliverable`. The endpoint paths below are relative to that context. This contract reflects the current admin frontend and runtime `AuthResponse` fields (`token`, `username`, `role`, and `message`); older examples in `specs/001-admin-login/contracts/authentication-api.md` describe a different response shape.

## Common rules

- Admin authentication and operational endpoints use the existing `/api/auth/**`, `/customer/**`, and `/account/**` paths.
- Admin credentials are independent from customer credentials. The admin API uses an explicitly supplied bearer token; it does not use or create the customer-authentication session cookie.
- The raw bearer token is returned only in a successful login response. It must not be returned in error responses, persisted in raw form, or recorded in logs or audit events.
- Requests to protected customer/account operations send `Authorization: Bearer <token>`. For compatibility, the server may continue accepting the existing raw-token header form if it already does.
- Unknown username and wrong password produce equivalent public failures. Error messages must not disclose whether a username/email exists or provide sensitive authentication details.
- Successful, validation, authentication, and authorization response bodies retain the existing safe fields. No raw password is ever returned.

## POST `/api/auth/login`

Authenticate a bank teller or bank manager.

**Request: `application/json`**

```json
{
  "username": "bank.teller",
  "password": "existing-password"
}
```

`username` may be either the account username or its email address.

**Success: `200 OK`**

```json
{
  "token": "opaque-random-bearer-token",
  "username": "bank.teller",
  "role": "BANK_TELLER",
  "message": "Login successful"
}
```

The raw token is provided only here for the existing client to store and use in the `Authorization` header. The persisted session stores only a one-way digest.

**Missing credentials: `400 Bad Request`**

```json
{
  "error": "validation_error",
  "message": "Username and password are required."
}
```

**Invalid credentials: `401 Unauthorized`**

```json
{
  "error": "authentication_failed",
  "message": "Invalid username or password."
}
```

**Temporarily locked or administratively disabled account: `423 Locked`**

```json
{
  "error": "account_locked",
  "message": "This account is temporarily locked or inactive. Contact an administrator."
}
```

The response must not disclose whether the account exists beyond the existing account-status contract. An account store or password-upgrade persistence failure must not create a token or authenticated session.

**Account store unavailable: `503 Service Unavailable`**

```json
{
  "error": "service_unavailable",
  "message": "The service is temporarily unavailable."
}
```

Malformed or missing request bodies return `400 Bad Request` with the safe `validation_error` category. Unexpected internal errors return a generic `500 Internal Server Error` response and must not expose internal exception details.

## GET `/api/auth/session`

Validate the current administrator session. The existing client supplies the bearer token in the `Authorization` header.

**Success: `200 OK`**

```json
{
  "token": null,
  "username": "bank.teller",
  "role": "BANK_TELLER",
  "message": "Session valid"
}
```

Successful validation updates the session's last-used timestamp and rolling 15-minute inactivity expiry. The `token` field remains for response-shape compatibility but is always null; the raw bearer token is not echoed after login.

**Missing, invalid, revoked, expired, disabled-owner, or non-admin session: `401 Unauthorized`**

```json
{
  "error": "session_expired",
  "message": "Your session has expired. Please sign in again."
}
```

## POST `/api/auth/logout`

Invalidate the current administrator session. The existing client supplies the bearer token in the `Authorization` header.

**Success: `200 OK`**

```json
{
  "token": null,
  "username": null,
  "role": null,
  "message": "Signed out successfully"
}
```

Repeated logout is idempotent and does not create or restore a session. It does not change customer authentication state.

## Protected `/customer/**` and `/account/**` operations

Every existing customer and account management operation requires a current active administrator session and an eligible teller or manager role.

- Missing, invalid, or expired token: `401 Unauthorized`.
- Authenticated principal without an eligible administrator role: `403 Forbidden`.
- Denial occurs before protected data is returned or mutated.
- An error response uses the safe `error` and `message` fields and never contains a bearer token.

## Compatibility and security expectations

- Preserve the three endpoint paths, login request fields, bearer-token header, and the successful `token`, `username`, `role`, and `message` fields.
- Status codes for authentication and authorization failures may become semantically specific as Spring Security enforcement is introduced; existing error/message fields remain available to the current client.
- CSRF protection for customer cookie sessions remains scoped to `/api/customer-auth/**`. Admin requests authenticate with an explicitly attached bearer header.
- CORS remains restricted to configured application origins. Credentialed wildcard origins are not allowed.
