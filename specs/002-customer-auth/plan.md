# Implementation Plan: Customer Authentication

**Branch**: `002-customer-auth` | **Date**: 2026-10-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from [spec.md](./spec.md)

## Summary

Add a public customer registration and sign-in journey, with a customer-only landing area, while preserving existing admin API behavior. Registration requires name, street number, postal code, city, province, email, E.164 phone number, and an 8+-character password. Generate and return a unique username from the lowercase name with whitespace removed and a duplicate sequence suffix (`001`, `002`, ...). Keep customer credentials and session state separate from admin authentication. Add Spring Security to the Spring Boot backend for customer authentication, adaptive password encoding, session management, CSRF protection, and authorization. Place all admin UI under `/admin/**` and customer-facing UI under `/customer/**`.

## Technical Context

**Language/Version**: Java 17, Spring Boot 4.1.1, React 19.2.8, Vite 8.3.0

**Primary Dependencies**: Spring Boot-managed `spring-boot-starter-security` (new dependency), existing Spring Data JPA and Spring Web MVC, MySQL Connector/J, React Router DOM, Vitest, Testing Library

**Storage**: Existing MySQL database; add a distinct customer identity table with unique generated username and normalized email. Store the registration address as part of the customer login identity without linking or creating the operational bank `Customer` or `Account`. Store Spring Security's `SecurityContext` in a server-managed HTTP session; do not reuse the admin `sessions` table or browser token.

**Testing**: Maven backend tests (existing Spring/JPA test dependencies); Vitest and Testing Library frontend tests; targeted API and browser-flow validation

**Target Platform**: Browser frontend and Spring Boot web service

**Project Type**: Web application with separate frontend and backend

**Performance Goals**: Meet the spec's customer access journey target of under two minutes in standard use. Tune the adaptive password encoder to deployment hardware. No additional API latency or throughput threshold is defined.

**Constraints**: The original feature description requests existing libraries only; this design proposes Spring Security as a justified dependency exception for password encoding, authentication, authorization, CSRF, and session protections that would otherwise be hand-rolled. Let Spring Boot manage the version. Keep `/api/auth/**` and existing admin authentication/API behavior intact. Use explicit ordered customer and fallback filter chains so the new starter does not silently secure or change unrelated existing endpoints. Never accept username, customer role, status, or admin privileges from registration input; enforce customer/admin authorization on the backend; do not expose password material. Enforce an 8-character minimum, support passwords of at least 64 characters, and do not impose composition rules. Apply temporary sign-in rate limiting without permanent account lockout. Customer sessions expire after 15 minutes idle.

**Scale/Scope**: Self-service customer registration, generated username, customer login/logout/session validation, and an empty customer landing page. Username suffixes begin at `001`, are at least three digits, and expand after `999`. Support concurrent registrations for the same normalized name base without duplicate usernames. Broader profile management, banking account workflows, password recovery, email verification, and admin account creation are out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The checked-in constitution is still the unfilled template: it contains placeholders rather than adopted principles or enforceable gates. There are therefore no active constitution rules to conflict with this design. The proposal follows the existing frontend/backend structure and does not merge the customer identity store with admin identity. Spring Security is a deliberate exception to the initial existing-libraries-only request, justified by the need for mature password encoding, server-side authentication/session handling, CSRF protection, and backend authorization without implementing these security mechanisms ad hoc.

**Pre-research gate**: PASS — no active constitution constraints; Spring Security is a justified dependency exception for authentication and authorization controls.

**Post-design gate**: PASS — the design stays within the existing application projects and test runners. A Spring Security starter is proposed with a Boot-managed version; ordered, explicitly scoped filter chains are required to prevent its default whole-application protection from changing existing admin behavior.

## Project Structure

### Documentation (this feature)

```text
specs/002-customer-auth/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── customer-authentication-api.md
├── spec.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
backend/src/main/java/com/fdmgroup/sprint4Deliverable/
├── Controller/
│   └── CustomerAuthController.java
├── Config/
│   └── CustomerSecurityConfig.java
├── Model/
│   └── CustomerAccount.java
├── Repository/
│   └── CustomerAccountRepository.java
└── Service/
    ├── CustomerAuthService.java
    └── CustomerUsernameService.java

backend/src/test/java/com/fdmgroup/sprint4Deliverable/
└── Controller, Service, and Security customer-auth tests

frontend/src/
├── components/
│   └── customer-only route guard
├── context/
│   └── customer auth state
├── pages/auth/
│   ├── CustomerLogin.jsx
│   └── CustomerRegistration.jsx
├── pages/customer/
│   └── CustomerHome.jsx
├── services/
│   └── customer auth API client
├── App.jsx
└── __test__/
    └── customer-auth flow tests
```

**Structure Decision**: Keep the existing split `backend/` and `frontend/` application. Add a dedicated customer-auth controller, account entity/repository, auth service, and username-generation service. Configure Spring Security with an ordered customer API filter chain and an explicit fallback chain preserving existing admin/business endpoint behavior. Admin UI paths move under `/admin/**`: `/admin/login`, `/admin/dashboard`, `/admin/customers`, `/admin/customers/add`, `/admin/customers/edit/:customerId`, `/admin/accounts`, `/admin/accounts/add`, and `/admin/accounts/edit/:accountId`. Customer UI paths use `/customer/login`, `/customer/register`, and `/customer/home`. The public `/` entry links to customer sign-in/registration and admin sign-in. Existing operational pages formerly under `/customer/**` and `/account/**` move under `/admin/**`; `/customer/**` is reserved for the customer-facing experience. Customer auth state/client and route guard remain separate from admin auth; both UI route families receive role-specific guards.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|--------------------------------------|
| Add Spring Security starter despite the original existing-libraries-only constraint | Adaptive password encoding, authentication, session fixation protection, CSRF defenses, and backend authorization are core security requirements; use the version managed by Spring Boot. | Hand-rolled password hashing, session rotation, CSRF, and authorization increase security risk and maintenance burden; JCA alone does not provide these surrounding controls. |
| Explicit fallback security filter chain | Spring Boot secures all web requests by default when Spring Security is on the classpath. | Accepting defaults could silently change existing admin APIs and unrelated endpoints. A customer-scoped chain plus compatibility fallback makes behavior explicit and testable. |
| Concurrent username allocation coordination | Generated usernames share a name-derived base and must stay unique for simultaneous registrations. | A query-then-insert allocator alone is race-prone; the database unique constraint and bounded retry handle collisions safely. |
