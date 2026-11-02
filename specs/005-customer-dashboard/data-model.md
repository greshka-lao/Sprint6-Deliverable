# Data Model: Customer Dashboard

The dashboard is a read-only view over existing customer, address, and account data. No database entities or persisted fields are added.

## Existing source entities

### Customer profile

Represents the customer resolved from the authenticated session principal.

| Field | Source | Dashboard behavior |
|---|---|---|
| Customer ID | `Customer.customerId` | Internal ownership key; not accepted from the request and not required in the customer-facing response |
| Name | `Customer.name` | Display when present |
| Email | `Customer.email` | Display when present; absent values are indicated as unavailable |
| Phone number | `Customer.phoneNumber` | Display when present; absent values are indicated as unavailable |
| Address | `Customer.address` → `Address` | Display available street number, city, province, and postal code components; do not substitute another customer's data |

### Bank account

Represents an existing account assigned to the authenticated customer through `Account.customer`.

| Field | Source | Dashboard behavior |
|---|---|---|
| Account ID | `Account.accountId` | Display as the account identifier |
| Account type | Joined subtype (`SavingsAccount` or `CheckingAccount`) | Display as Savings or Checking |
| Balance | `Account.balance` (`double`) | Preserve the existing stored value and display to two decimal places |
| Currency | Not persisted | Treat as the platform's single currency, NZD, consistent with the existing `en-NZ` display convention |
| Ownership | `Account.customer` | Include only accounts linked to the authenticated customer |

## Dashboard response view

The endpoint returns a response projection assembled from the authenticated customer and their linked accounts:

- **Profile**: name, email, phone number, and address components. Individual fields/components may be absent.
- **Accounts**: zero or more account projections, each containing account ID, type, balance, and currency.
- **Total**: sum of the returned account balances in NZD, rounded/formatted to two decimal places. An empty account list has a zero total.

The total is derived from the same account collection returned in the response. Aggregate existing stored balances using decimal arithmetic; do not change the account table or its balance column as part of this feature.

## Validation and state rules

- The principal must be authenticated as a customer before a dashboard response is produced.
- The server resolves the customer using the authenticated principal name; no caller-supplied identity participates in lookup.
- Include only accounts whose customer relationship is the resolved customer.
- A customer with no accounts receives an empty list and a zero total.
- Missing profile values remain missing in the response and are represented as unavailable in the UI.
- A dashboard read failure is an error state, not an empty-account response or a zero-balance fallback.
- All included accounts use the existing single platform currency assumption. Currency conversion is not supported.
