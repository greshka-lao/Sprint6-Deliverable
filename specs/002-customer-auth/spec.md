# Feature Specification: Customer Authentication

**Feature Branch**: `002-customer-auth`

**Created**: 2026-10-27

**Status**: Draft

**Input**: User description: "Refactor the existing codebase with a React frontend, Spring Boot backend and MySQL database to add a customer log-in and registration page. Use existing project libraries only. The frontend should follow the same design as the current UI, especially Header and Sidebar components. Add API endpoints for this story if required. There is already an admin log-in page which is separate to the customer log-in and registration page. Admins cannot create their own account - only customers. There are no features in this platform for customer (all admin), but the app should be scalable for future customer-facing features."

## Clarifications

### Session 2026-10-29

- Q: Which address fields should customers provide during registration? → A: Use the current project shape: street number, postal code, city, and province.
- Q: What phone-number format should registration accept and store? → A: Require country code; validate and store in E.164 format.
- Q: What minimum password length should customer registration enforce? → A: Minimum 8 characters; allow 64+; no composition rules.
- Q: How should the system respond to repeated failed customer sign-in attempts? → A: Temporarily rate-limit repeated attempts; do not lock accounts permanently.
- Q: How long should a customer stay signed in without activity before the session expires? → A: 15 minutes of inactivity.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create a customer account (Priority: P1)

A person who wants to use the platform as a customer can register for an account without needing admin assistance. The registration journey should feel familiar to the current application and must clearly separate customer onboarding from the internal admin sign-in flow.

**Why this priority**: This is the foundation for all future customer-facing functionality and ensures the product can serve non-admin users safely and consistently.

**Independent Test**: A customer can open the registration page, complete the required information, submit it successfully, and then sign in with the same credentials without help from an administrator.

**Acceptance Scenarios**:

1. **Given** a prospective customer is on the public entry screen, **When** they choose to register, **Then** they are taken to a dedicated customer registration form and can complete the process.
2. **Given** a customer enters valid registration details, **When** they submit the form, **Then** their account is created and they are directed to a successful sign-in or onboarding flow.
3. **Given** a customer enters an email address that is already registered, **When** they submit the form, **Then** they receive a clear duplicate-email message and must correct it before continuing.
4. **Given** multiple customers register with the same name, **When** each registration succeeds, **Then** each receives a distinct username with an incremented numeric suffix.

---

### User Story 2 - Sign in as a customer (Priority: P1)

A registered customer needs a simple and reliable way to access the customer area of the application. The customer sign-in flow must be different from the admin sign-in flow and must protect the platform from account confusion or privilege escalation.

**Why this priority**: Customer access is the primary business need for this release, and successful sign-in is required before any future customer-facing features can be used.

**Independent Test**: A registered customer can enter their username and password on the customer sign-in page and access the customer area without affecting admin workflows.

**Acceptance Scenarios**:

1. **Given** a customer has a valid registered account, **When** they enter the correct credentials, **Then** they are signed in and transferred to the customer experience.
2. **Given** a customer enters incorrect credentials, **When** they submit the sign-in form, **Then** they receive an error message and remain on the sign-in page.
3. **Given** an admin account is in use, **When** a user attempts to sign in through the customer flow, **Then** the system does not treat that as a valid customer account unless it is explicitly created as a customer account.

---

### User Story 3 - Maintain separation between admin and customer access (Priority: P2)

The platform already supports admin-only operations, but the introduction of customer access requires clear separation of user types. Admins should continue to manage the internal system while customers have a self-service path.

**Why this priority**: A strong separation between user classes reduces confusion, protects operational workflows, and makes the application safe to expand for future customer-facing features.

**Independent Test**: Admin and customer experiences remain distinct, and self-service account creation is limited to customer accounts only.

**Acceptance Scenarios**:

1. **Given** the application is running with both admin and customer flows, **When** an admin attempts to create a new account from the admin interface, **Then** that action is not available or is blocked by policy.
2. **Given** a customer account exists, **When** the customer signs in, **Then** they can access only customer-facing content and not admin-only tools.
3. **Given** future customer features are added, **When** the system is extended, **Then** the customer account model is ready to support additional responsibilities without replacing the admin model.

---

### Edge Cases

- What happens when a customer tries to sign in before completing registration?
- What happens when a customer submits an email that is already registered?
- How are generated usernames allocated when concurrent registrations use the same name?
- What happens when an admin user attempts to access the customer registration flow?
- How does the system handle an empty, malformed, or unusually long submission?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a dedicated customer registration page that is separate from the admin sign-in experience.
- **FR-002**: The system MUST allow a customer to create an account using a valid set of required details and must clearly communicate any validation or duplicate-record issues.
- **FR-003**: The system MUST allow a registered customer to sign in using their account credentials and access the appropriate customer experience.
- **FR-004**: The system MUST keep admin and customer authentication flows distinct so that admin access remains separate from customer access.
- **FR-005**: The system MUST prevent self-service account creation from being used to create admin accounts or grant admin privileges.
- **FR-006**: The system MUST support customer account state and role separation in a way that can expand to future customer-facing features without altering the admin flow.
- **FR-007**: The system MUST provide clear user feedback for registration and sign-in errors so customers understand what needs to be corrected.
- **FR-008**: The system MUST be usable by customers without requiring internal admin intervention during the standard sign-in and registration process.
- **FR-009**: Customer registration MUST require the customer's name, address (street number, postal code, city, and province), email address, phone number in international E.164 format with a country code, and password. Passwords MUST be at least 8 characters; the system MUST allow passwords of at least 64 characters and MUST NOT require arbitrary character-composition rules. The username MUST be generated by the system from the name by converting it to lowercase, removing all whitespace, and appending a numeric sequence beginning at `001` for each resulting name base (for example, `Greshka Lao` becomes `greshkalao001`, then `greshkalao002`). The suffix MUST be padded to at least three digits and expand beyond `999`. The generated username MUST be unique and returned to the customer on successful registration.
- **FR-010**: The system MUST temporarily rate-limit repeated failed customer sign-in attempts, return a generic authentication failure, and MUST NOT permanently lock a customer account solely because of failed sign-in attempts.
- **FR-011**: A customer authentication session MUST expire after 15 minutes of inactivity.

### Key Entities *(include if feature involves data)*

- **Customer Account**: Represents a user who is allowed to use the customer-facing experience; includes a generated unique username, name, address, email, phone number, password hash, account status, and role designation.
- **Admin Account**: Represents an internal user with operational permissions; remains distinct from customer accounts and is not created through the self-service customer registration path.
- **Authentication Session**: Represents the state of a signed-in user and determines which area of the application they can access.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Customers can complete the registration process and successfully sign in without needing support from the admin team in at least 95% of test attempts.
- **SC-002**: Customers can access their intended customer experience from the sign-in flow in under 2 minutes during standard usage.
- **SC-003**: No customer account creation path grants admin privileges or allows unauthorized creation of admin accounts.
- **SC-004**: The application successfully distinguishes customer and admin access in all core account journeys and remains ready for future customer-facing feature expansion.
- **SC-005**: Users receive clear validation and error feedback for duplicate, missing, or invalid account details during registration and sign-in.

## Assumptions

- Customer accounts are intended for self-service access and are not created by admins as part of this feature.
- The platform already has a separate internal admin flow that remains active for operational users.
- The initial customer experience is intentionally small but designed with future customer-facing features in mind.
- The initial account model will include the customer name, address, email, phone number, and secure password data needed for signup and sign-in. Usernames are generated from names and shown to customers for subsequent sign-in.

## 5.2 Scope and Non-Goals

### Scope

- Provide a dedicated customer sign-in experience separate from the admin workbench.
- Provide a customer registration journey for first-time users.
- Clearly separate customer and admin access so both user groups can operate safely on the same platform.
- Support a foundation for future customer-facing features without redesigning the admin experience.

### Non-Goals

- Admin self-service account creation.
- Password reset, forgot-password, or account recovery flows.
- Ongoing customer profile management, billing, or order workflows beyond collecting the required name and contact/address details during registration.
- Replacing or merging the current admin role model with a single account type.

## 5.3 Actors and Preconditions

### Actors

- **Customer**: A person who wants to create a personal account and use the platform as a customer.
- **Admin**: An internal staff member who manages the platform and uses the separate admin sign-in flow.
- **System**: The application responsible for validating account requests, managing sign-in, and enforcing access boundaries.

### Preconditions

- The app already contains a functioning admin sign-in experience.
- The customer-facing journey is available through the same web application and follows the existing application styling and navigation patterns.
- The user has either not yet registered or has already created a valid customer account.
- The platform is expected to support multiple user classes without mixing customer and admin privileges.

## 5.4 Core Behavior and Flows

### Customer registration flow

1. A new customer opens the customer registration page from the app entry experience.
2. The customer enters their name, street number, postal code, city, province, email address, phone number in E.164 format, and secure password.
3. The system validates the required information, generates a unique username from the normalized name, and confirms whether the account can be created.
4. If valid, the system creates the customer account and provides a successful confirmation or next-step guidance.
5. The system displays the generated username, and the customer can sign in using that username and their password.

### Customer sign-in flow

1. A registered customer opens the customer sign-in page.
2. The customer enters their username and password.
3. The system validates the account against the stored customer credentials.
4. If valid, the customer is directed to the customer experience.
5. If invalid, the customer remains on the sign-in page and receives a clear error message.

### Access separation flow

1. Admin and customer paths remain distinct at every step of the user journey.
2. Customer access is limited to customer-facing capabilities and does not grant admin privileges.
3. Admin-only operations remain restricted to admin accounts and the admin workflow.

## 5.5 Business Rules

- Only customers may create customer accounts through the self-service registration process.
- Admin accounts are not created through the customer registration flow.
- Generated usernames must be unique. For the same normalized name base, assign an incrementing numeric suffix starting at `001`, padded to at least three digits; continue with additional digits after `999`.
- Email addresses must be unique within the customer account store.
- Phone numbers must include a country code, pass E.164 validation, and be stored in E.164 format.
- All required account information must be provided before a registration is accepted.
- Sign-in attempts for invalid credentials must fail cleanly without exposing sensitive account information.
- Customer access must not unlock internal admin tools, dashboards, or account-management capabilities.
- Customer sessions expire after 15 minutes without activity.
- The customer account model must be extensible for future customer-facing features without altering the admin model.

## 5.6 Error Conditions and Edge Cases

- A customer attempts to register with an already used email.
- Multiple customers register with names that normalize to the same username base; each must receive a distinct incremented username.
- A customer submits a form with missing or malformed details.
- A customer submits a phone number without a country code or in an invalid format.
- A customer attempts to sign in with an incorrect password or unknown username.
- A customer makes repeated failed sign-in attempts and is temporarily rate-limited without permanent account lockout.
- A customer session expires after 15 minutes without activity and requires sign-in again.
- An admin user attempts to open or use the customer registration path.
- A customer tries to access admin-only pages without valid admin access.
- Request data is unusually long, malformed, or incomplete during customer sign-in or registration.
- The system must provide clear guidance when an account cannot be created or validated.

## 5.7 Non-Functional Requirements (NFRs)

- The customer experience must be easy to understand and complete with minimal friction.
- Feedback for validation and login failures must be clear, specific, and actionable.
- The interface must remain consistent with the current design language, including navigation and page structure.
- Account access should be predictable and secure, with clear separation between customer and admin roles.
- The customer entry points should support future scale without requiring a redesign of the account model.
- The system should remain reliable under normal business usage, with account flows completing without unnecessary delays or uncertainty.

## 5.8 API Expectations (High-Level)

- The system should expose high-level account APIs to support customer registration, customer sign-in, and account validation.
- Registration requests should accept name, structured address, email, phone number, and password; the server generates and returns the customer's username.
- Registration should return a clear success or validation failure outcome, including a duplicate-email response when applicable.
- Sign-in requests should validate supplied credentials and return an appropriate result indicating whether access is granted or denied.
- The API layer should support distinguishing customer accounts from admin accounts and avoid granting admin privileges through customer flows.
- Responses should communicate clear status outcomes so the user experience can show accurate success or error messaging.
- The account model should allow future extension for additional customer-facing features without breaking the existing admin flow.
