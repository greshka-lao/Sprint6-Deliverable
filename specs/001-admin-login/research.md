# Research: Admin Login Access Model

## Decisions

### Decision: Use a role-based admin access model
The platform will use a role-based permission model rather than a single admin-only flag. The current release defines two admin roles: bank teller and bank manager. Both roles share the same permissions for the existing customer and account management actions already present in the system.

**Rationale**
- This aligns with the requirement that the platform remain scalable for future customer-facing roles.
- It keeps the sign-in model decoupled from feature-specific permissions.
- It matches the existing application structure where admin functions are grouped under shared workflows.

**Alternatives considered**
- Hardcoded admin credentials: rejected because it does not scale and does not fit a role-based access model.
- Separate customer and admin login experiences from the start: rejected because it adds unnecessary complexity before the app needs customer-facing flows.

### Decision: Use database-backed test admin records for validation
A temporary test identity layer will be created using database-backed admin user records so the login and permission checks behave like the real application without requiring a production-grade external identity provider.

**Rationale**
- It validates the same access-control flow as a real admin identity system.
- It supports testing of role-based permissions and authorization flows.
- It is easier to evolve into a full production identity model later.

**Alternatives considered**
- Hardcoded credentials in frontend or backend code: rejected because it is insecure and difficult to manage.
- No temporary login: rejected because it blocks feature validation before a full identity system is available.

### Decision: Protect admin routes by session + role enforcement
Protected pages must validate both session existence and role eligibility before access is granted.

**Rationale**
- It prevents direct access to admin-only content by unauthenticated or unauthorised users.
- It matches the existing business rule that admin functions are accessible only to active admin roles.
- It supports future permissions expansion without changing the sign-in flow.

## Findings

- The existing web application is organized as a React frontend and Spring Boot backend using MySQL persistence through JPA.
- The current application already contains admin-style operational flows for customers and accounts, so the login feature should secure those existing actions rather than introducing a separate access surface.
- The app already has a shared header and sidebar pattern, so the login page should follow the same UI conventions while remaining clearly distinct as a sign-in screen.
- The temporary login approach should remain clearly labelled as test-only and should not be treated as the final production system.

## Open questions resolved

- The system needs a role-based access model for future growth: resolved.
- The current admin roles are bank teller and bank manager: resolved.
- The current permissions are shared between these roles: resolved.
- Temporary testing sign-in should use database-backed records: resolved.
