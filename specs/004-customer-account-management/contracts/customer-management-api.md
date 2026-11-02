# Customer Management API Contract

Backend requests are served under the existing servlet context path `/sprint4Deliverable`. Existing customer authentication endpoints use `/api/customer-auth`; administrator customer endpoints use `/customer`. Customer and administrator authorization continue to use their existing separate security boundaries.

This contract supersedes the separate-account-store and no-operational-customer-link behavior documented in `specs/002-customer-auth/contracts/customer-authentication-api.md` for the endpoints and data model changed by this feature.

## Common rules

- Customer type is `person` or `company`; it identifies the matching joined subtype and cannot be changed through customer update.
- The customer list returns only records with `ACTIVE` status.
- Customer list rows contain `customerId`, `name`, `username`, concatenated `address`, `customerType`, `phoneNumber`, and `email`. Missing unclaimed-account contact/login fields are returned as `null` or an empty display value, never from another customer.
- Password hashes, raw claim references, session internals, and persistence-only fields are never serialized.
- Customer registration, claim, and claim completion are CSRF-protected state-changing requests. Credentialed browser calls use the existing customer-auth session and configured CORS policy.
- Administrative create, update, delete, and claim review require the existing authorized administrator role and bearer-token rules.

## Customer registration

### POST `/api/customer-auth/register`

Accepts `name`, `customerType`, `address` (`streetNumber`, `postalCode`, `city`, `province`), `email`, `phoneNumber`, and `password`. The server generates `username`, assigns the customer role/status, and persists the matching `Person` or `Company` joined subtype for a new customer.

If an exact matching administrator-created profile is found, the endpoint does not attach credentials or create a duplicate. It returns a claim-required response and directs the customer to request a claim.

### POST `/api/customer-auth/claims`

Accepts the customer's name, selected type, complete address, email, and phone number; does not accept a password. It returns `202 Accepted` and a single-use opaque `claimReference` only when there is an exact, unique matching unclaimed profile. The claim begins in `PENDING`; a failed or ambiguous match returns a generic response that does not disclose profile existence.

### GET `/api/customer-auth/claims/status`

Returns the request state (`PENDING`, `APPROVED`, or `REJECTED`) to the holder of a valid reference supplied in the `X-Customer-Claim-Reference` header. It does not expose customer profile data. Invalid or consumed references return a generic not-found/invalid response.

### POST `/api/customer-auth/claims/complete`

Available only after approval. Requires the claim reference in the `X-Customer-Claim-Reference` header and accepts the customer's password; attaches the verified claim email and phone, generates a unique username, and makes the account usable under the existing customer sign-in rules. Returns `customerId`, `name`, `username`, and a sign-in message. The reference is consumed on success. Do not put the reference in a URL or access log.

### Existing login/session endpoints

`POST /api/customer-auth/login`, `GET /api/customer-auth/session`, and `POST /api/customer-auth/logout` retain their existing request/response and session-security contract. Only active customers with completed credentials can authenticate.

## Administrator customer operations

Existing paths are retained for integration compatibility:

- `POST /customer/save`: create a customer profile with required `customerType`, name, and address. The profile has no customer login credentials until claimed.
- `GET /customer/getAll`: return active customer rows with the seven specified list fields; `address` is formatted as street number, city, province, postal code and the service applies the active filter.
- `GET /customer/get?customerId={id}`: return one customer detail to an administrator.
- `PUT /customer/edit`: update permitted profile/contact fields. Ignore or reject a submitted `customerType` change; never change type or joined subtype.
- `DELETE /customer/delete?customerId={id}`: require the existing explicit frontend confirmation; clear customer credentials, unassign bank accounts, and mark the central customer `DELETED` while retaining its joined person/company subtype row.
- `GET /customer/claims`: list pending claim requests for an authorized administrator.
- `POST /customer/claims/{claimRequestId}/approve`: approve one pending request without setting a password or attaching credentials.
- `POST /customer/claims/{claimRequestId}/reject`: reject one pending request without changing its customer profile.

Unknown customer/request IDs return a not-found response. Invalid input returns a clear validation response. Failed claim review attempts do not change the claim or profile.

## Admin customer-list UI

The canonical user-facing route is `/admin/customer`. Rows display ID, name, username, concatenated address, customer type, phone, and email, in that order. Address display order is street number, city, province, postal code. The existing plural customer route may redirect to the canonical route.
