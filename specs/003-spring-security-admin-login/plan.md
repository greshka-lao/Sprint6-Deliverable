# Implementation Plan: Spring Security for Admin Login

**Branch**: `003-spring-security-admin-login` | **Date**: 2026-10-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from [spec.md](./spec.md)

## Summary

Integrate Spring Security into the existing administrator authentication flow while preserving the public admin API contract and React client behavior. Use an administrator-only authentication provider backed by existing administrator records, upgrade legacy credentials after successful verification, and establish Spring Security authentication for protected administrator requests. Retain the existing opaque database-backed bearer token contract by resolving it into a per-request administrator principal; do not make admin requests depend on the customer HTTP-session security context. Add an ordered admin security chain between the existing customer chain and the compatibility fallback, protect existing customer/account operations, preserve the current 15-minute inactivity and five-failure lockout policies, and retain separate customer/admin credentials and sessions.

## Technical Context

**Language/Version**: Java 17; React 19.2.8 and Node.js 20+ for the existing sign-in client (no planned UI redesign)

**Primary Dependencies**: Spring Boot 4.1.1 dependency management, Spring MVC, Spring Data JPA, Spring Security 7.1.1 (starter already declared in `backend/pom.xml`), MySQL Connector/J, Maven; React Router and Vitest for client regression coverage

**Storage**: Existing MySQL tables/entities for `AdminUser`, `Role`, `Session`, and `AccessAttempt`, plus a narrow `AdminSecurityEvent` audit record for logout, lockout, expiry, and denied access; protect existing account data from destructive schema recreation

**Testing**: Maven/JUnit and Spring MockMvc integration tests for authentication, filter-chain isolation, protected endpoints, session lifecycle, and existing admin API behavior; existing frontend Vitest suite as regression coverage

**Target Platform**: Existing browser-based internal banking application with a Spring Boot backend under `/sprint4Deliverable`

**Project Type**: Web application with separate `backend/` and `frontend/` projects

**Performance Goals**: At least 95% of valid admin sign-ins complete within five seconds under normal operating conditions, as specified by SC-002

**Constraints**: Preserve `/api/auth/login`, `/api/auth/session`, `/api/auth/logout`, their documented public fields, and the `Authorization` bearer-token behavior; isolate admin security from `/api/customer-auth/**`; keep the five-failure/15-minute temporary lock and 15-minute session inactivity behavior; support first-success legacy password upgrade and fail closed if upgrade persistence fails

**Scale/Scope**: Current teller/manager roles and existing customer/account operations; no explicit concurrent-user or request-volume target is provided

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The repository's constitution file contains only unfilled template placeholders and defines no active project principles or governance rules. No constitution gate can be assessed as failed. The feature remains within the existing backend architecture, retains established API/client behavior, and separates the administrator security boundary from customer authentication.

## Project Structure

### Documentation (this feature)

```text
specs/003-spring-security-admin-login/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── admin-authentication-api.md
├── spec.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/fdmgroup/sprint4Deliverable/
    │   │   ├── Config/
    │   │   │   ├── AdminSecurityConfig.java
    │   │   │   ├── ApiExceptionHandler.java
    │   │   │   └── CustomerSecurityConfig.java
    │   │   ├── Controller/
    │   │   │   ├── AccountController.java
    │   │   │   ├── AuthController.java
    │   │   │   └── CustomerController.java
    │   │   ├── Model/
    │   │   │   ├── AccessAttempt.java
    │   │   │   ├── AdminSecurityEvent.java
    │   │   │   ├── AdminUser.java
    │   │   │   ├── AuthResponse.java
    │   │   │   ├── Role.java
    │   │   │   └── Session.java
    │   │   ├── Repository/
    │   │   │   ├── AccessAttemptRepository.java
    │   │   │   ├── AdminUserRepository.java
    │   │   │   ├── AdminSecurityEventRepository.java
    │   │   │   ├── RoleRepository.java
    │   │   │   └── SessionRepository.java
    │   │   ├── Security/
    │   │   │   └── AdminBearerTokenFilter.java
    │   │   └── Service/
    │   │       ├── AuthService.java
    │   │       ├── AuthorizationService.java
    │   │       └── AdminSecurityEventService.java
    │   └── resources/
    │       ├── application.properties
    │       └── application-development.properties
    └── test/java/com/fdmgroup/sprint4Deliverable/
        ├── Config/AdminAuthSecurityTests.java
        └── Service/AuthServiceTests.java

frontend/
├── src/
│   ├── context/AuthContext.jsx
│   ├── pages/auth/AdminLogin.jsx
│   └── services/authService.js
└── src/__test__/
```

**Structure Decision**: Keep authentication changes in the existing Spring Boot backend's configuration, service, repository, and test packages. The frontend is an API consumer whose existing token storage, `Authorization` header, login form, and routes are preserved; only changes required by verified contract compatibility should be made.

The implementation adds `SessionTokenMigrationRunner` under `Config/` and `AdminUserDetails`, `AdminUserDetailsService`, `LegacyAwarePasswordEncoder`, and `TokenDigest` under `Security/`. The production profile validates a schema provisioned through the deployment's managed migration process.

## Architecture and Integration

- Keep the customer authentication filter chain scoped to `/api/customer-auth/**` and at higher priority than the new admin chain. Preserve its current CSRF, CORS, principal, and session behavior.
- Add an admin chain after the customer chain and before the fallback, scoped to `/api/auth/**`, `/customer/**`, and `/account/**`. Permit the documented login, session-probe, and idempotent logout routes to reach their controller handlers; require an authenticated administrator principal for operational customer/account requests.
- Retain the compatibility chain last for unrelated legacy endpoints. It must not be able to bypass the admin matcher or customer-auth matcher.
- Use an explicitly admin-scoped `DaoAuthenticationProvider` and user lookup based on `AdminUserRepository.findByUsernameOrEmail`; grant authorities only to eligible `BANK_TELLER` or `BANK_MANAGER` accounts. Avoid relying on an unqualified global `UserDetailsService` or shared customer `AuthenticationManager` configuration.
- Publish separately qualified `ProviderManager` instances for customer and administrator authentication. The customer manager remains primary for framework setup, while admin login receives only the admin manager and provider.
- Authenticate passwords through Spring Security abstractions. Use a legacy-aware `PasswordEncoder` and `UserDetailsPasswordService` so a successful legacy credential check replaces the stored password with a delegating adaptive hash before session-token issuance; deny sign-in and issue no session if persistence fails. New and upgraded credentials use the configured adaptive encoder.
- Preserve the current random opaque token returned only by successful login, existing single-active-session behavior, and response fields. Store only a one-way digest of the random bearer token in `Session`; lookups, logout, and audit records must never persist or log the raw token. The admin Spring Security chain is stateless with respect to servlet `HttpSession`; a narrow bearer-token filter hashes the presented token, resolves it against the session repository, checks revocation/expiry/account-enabled/role state, refreshes the 15-minute idle expiry, and populates the request's Spring Security context. Generate a fresh token at successful login and revoke the previous token so token rotation protects against fixation.
- Keep login, logout, and session validation in the existing `/api/auth/**` endpoints and `AuthService` lifecycle. The controller delegates password verification to the admin `AuthenticationManager`; the existing service continues to issue and revoke database-backed tokens. Reuse existing login-attempt records for failure history and add an audit record for successful sign-in, sign-out, lockout, expiry, and denied admin access.
- Use JSON entry-point and access-denied handlers for REST endpoints. Preserve safe existing response fields (`error` and `message` where applicable), return unauthorized for missing/invalid authentication and forbidden for an authenticated non-admin principal, and do not expose account existence or internal failures. Return the bearer token only in the successful login response, as required by the existing client contract.
- Keep the credentialed customer CSRF policy scoped to the customer chain. Admin API authorization continues to require a bearer header explicitly attached by the existing client; it is not authenticated by an ambient cookie, so CSRF handling remains distinct from the customer session flow. Keep CORS origins restricted and do not enable credentialed wildcard origins.
- Preserve `AdminUser`/`Role`/`Session` associations. Ensure temporary lock expiry is distinct from administrator-disabled state; after five consecutive failures, a temporary lock lasts 15 minutes and clears automatically, whereas administratively disabled accounts remain disabled. Do not overload the `enabled` flag to represent a temporary lock.
- Stop using destructive schema recreation for this feature's persistent account/session data. In local development, retain existing database rows across application restarts and seed only development accounts under an explicit development profile; production startup must not create known default teller/manager credentials. Document that production schema changes require a managed migration process rather than relying on automatic destructive DDL.
- Persist a one-way digest of the random bearer token in the existing session token column; hash the presented token before lookups or revocation. A successful session-validation response must not echo the raw token; retain the existing token field as null if needed for response DTO shape compatibility.
- On startup, migrate legacy raw administrator session-token column values in place to SHA-256 digests so existing sessions remain usable without leaving raw tokens at rest; the migration must be idempotent and preserve already-digested values.
- Verify and update API contract documentation against actual current response DTOs before changing behavior. Do not silently adopt stale examples that disagree with the existing admin client's `token`, `username`, `role`, and `message` fields.

## Constitution Check (Post-Design)

The post-design gate passes: the plan uses existing project libraries, preserves the existing admin API/client behavior except for the clarified removal of token echo from session validation, isolates customer and administrator identity/session boundaries, and makes no unjustified project-structure expansion. The repository constitution remains an unfilled template without active gates.

## Complexity Tracking

No constitution violations are identified. The bearer-token bridge and ordered chains are required to integrate framework-level enforcement without breaking the existing admin client contract or customer authentication behavior.
