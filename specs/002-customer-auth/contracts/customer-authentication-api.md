# Customer Authentication API Contract

The backend is hosted under the configured servlet context path `/sprint4Deliverable`. The endpoints below use the base path `/sprint4Deliverable/api/customer-auth`. JSON request/response fields are the complete public contract; credentials, session internals, and persistence entities must not be serialized.

## Common rules

- Customer endpoints consult only customer account/session state. Existing `/sprint4Deliverable/api/auth/**` behavior remains the admin contract.
- Spring Security's higher-priority filter chain applies to customer-auth endpoints and future customer-only APIs; a lower-priority compatibility chain preserves the current behavior outside that scope. Do not rely on Spring Boot's generated default user.
- Registration and sign-in are anonymous; session validation requires an authenticated `ROLE_CUSTOMER` principal. A customer principal is never accepted as an admin principal.
- Registration requires name, address, email, phone number, and password. It never accepts a username, role, status, admin flag, session identifier, or operational customer/account ID.
- The address has `streetNumber`, `postalCode`, `city`, and `province` fields. Phone numbers include a country code and are validated and stored in E.164 format.
- The server generates a username by lowercasing the name, removing all whitespace while preserving other characters, and appending a sequence beginning at `001` for each matching base. The suffix is at least three digits and expands beyond `999`; allocation is unique even for concurrent registrations.
- Normalized email and generated username values are unique within the customer account store.
- Passwords are submitted only over HTTPS in deployed environments; the server stores only an algorithm-prefixed adaptive hash created with Spring Security `PasswordEncoder`.
- Passwords must have at least 8 characters; passwords of at least 64 characters must be supported. No arbitrary character-composition rules apply.
- Unknown username and wrong password produce the same generic sign-in failure.
- Repeated failed customer sign-in attempts are temporarily rate-limited without permanently locking the account. A rate-limited response must not reveal whether the username exists.
- State-changing requests (`POST`) require the Spring Security CSRF token in `X-CSRF-TOKEN`. Fetch a fresh token after successful login if the security configuration rotates/clears the anonymous token.
- Browser requests include credentials. CORS must allow only configured frontend origins and credentials; wildcard origin is not valid with credentialed requests.
- Spring Security's session-fixation protection rotates the session identifier on successful login. The session cookie is `HttpOnly`, uses an appropriate `SameSite` policy, and is `Secure` in HTTPS deployments.
- Customer sessions expire after 15 minutes of inactivity.

## GET `/csrf`

Issue or return a CSRF token for the current anonymous/session context.

**Success: `200 OK`**

```json
{
  "token": "session-bound-csrf-token"
}
```

The client sends the token in `X-CSRF-TOKEN` on registration, login, and logout requests.

## POST `/register`

Create a customer account. This does not create or link an admin or operational bank-customer record. The server generates a username and returns it so the customer can use it at sign-in.

**Request: `application/json`**

```json
{
  "name": "Greshka Lao",
  "address": {
    "streetNumber": "123 Main Street",
    "postalCode": "A1A 1A1",
    "city": "Example City",
    "province": "Example Province"
  },
  "email": "greshka@example.com",
  "phoneNumber": "+15551234567",
  "password": "a-long-customer-password"
}
```

**Success: `201 Created`**

```json
{
  "id": 42,
  "name": "Greshka Lao",
  "username": "greshkalao001",
  "email": "greshka@example.com",
  "phoneNumber": "+15551234567",
  "message": "Registration successful. Please sign in."
}
```

**Validation failure: `400 Bad Request`**

```json
{
  "code": "validation_error",
  "message": "Enter all required details and correct any invalid values."
}
```

**Duplicate identity: `409 Conflict`**

```json
{
  "code": "duplicate_identity",
  "message": "That email address is already in use."
}
```

The next account with the same normalized name base receives `greshkalao002`; numbering is padded to three digits and expands naturally after `999`. Enforce email and generated-username uniqueness with database constraints as well as friendly pre-checks; handle collisions from concurrent allocations without returning a duplicate username.

## POST `/login`

Authenticate a customer and establish an HTTP session. Successful login rotates the session identifier.

**Request: `application/json`**

```json
{
  "username": "greshkalao001",
  "password": "a-long-customer-password"
}
```

**Success: `200 OK`** (also sets an `HttpOnly` session cookie)

```json
{
  "id": 42,
  "username": "greshkalao001",
  "role": "CUSTOMER"
}
```

**Failure: `401 Unauthorized`**

```json
{
  "code": "authentication_failed",
  "message": "Invalid username or password."
}
```

No difference in response is made between an unknown account and a wrong password. Invalid customer credentials do not authenticate an admin.

**Rate limited: `429 Too Many Requests`**

```json
{
  "code": "rate_limited",
  "message": "Sign-in is temporarily unavailable. Please try again later."
}
```

Include a `Retry-After` header. Keep the response generic and do not reveal whether the submitted username exists. This rate limit is temporary and does not permanently lock the customer account.

## GET `/session`

Validate the current customer session.

**Success: `200 OK`**

```json
{
  "authenticated": true,
  "customer": {
    "id": 42,
    "username": "greshkalao001",
    "role": "CUSTOMER"
  }
}
```

**Missing, expired, or non-customer session: `401 Unauthorized`**

```json
{
  "authenticated": false,
  "code": "session_expired",
  "message": "Your session has expired. Please sign in again."
}
```

## POST `/logout`

Invalidate the current customer session and expire its cookie.

**Success: `204 No Content`**

Calling customer logout does not affect an admin session.
