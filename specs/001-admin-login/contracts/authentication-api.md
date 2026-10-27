# Authentication API Contract

## POST /api/auth/login

### Purpose
Authenticate a bank teller or bank manager and create an admin session.

### Request body
```json
{
  "username": "bank.teller",
  "password": "Password123!"
}
```

### Success response
```json
{
  "success": true,
  "message": "Login successful",
  "user": {
    "id": 1,
    "username": "bank.teller",
    "role": "BANK_TELLER"
  },
  "session": {
    "expiresAt": "2026-10-23T15:40:00Z"
  }
}
```

### Error responses
```json
{
  "success": false,
  "code": "authentication_failed",
  "message": "Invalid username or password."
}
```

```json
{
  "success": false,
  "code": "validation_error",
  "message": "Username and password are required."
}
```

## POST /api/auth/logout

### Purpose
Terminate the current authenticated session.

### Request headers
- Authorization: current session token or equivalent session identifier

### Success response
```json
{
  "success": true,
  "message": "Logout successful"
}
```

## GET /api/auth/session

### Purpose
Validate whether the current session remains active and authorised.

### Success response
```json
{
  "authenticated": true,
  "role": "BANK_MANAGER",
  "permissions": [
    "CUSTOMER_MANAGE",
    "ACCOUNT_MANAGE"
  ]
}
```

### Failure response
```json
{
  "authenticated": false,
  "code": "session_expired",
  "message": "Your session has expired. Please sign in again."
}
```

## Contract rules

- Only documented request and response fields are considered valid API contract fields.
- Undocumented fields must be rejected or ignored consistently.
- Sensitive values such as raw passwords must never be returned in responses.
- Authentication failures must never disclose whether a specific username exists unless the business rules explicitly allow it.
