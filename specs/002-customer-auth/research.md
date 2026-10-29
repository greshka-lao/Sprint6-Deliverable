# Research: Customer Authentication

## Decisions

### Keep customer and admin identity paths separate

**Decision**: Add a customer-only account store and auth endpoints under `/api/customer-auth/**`; leave the existing `/api/auth/**`, `AdminUser`, and admin session behavior unchanged. Move the admin UI routes under `/admin/**` as specified, and place the customer UI under `/customer/**`. Customer login looks up only customer accounts. Registration assigns the customer role on the server and accepts no role, admin flag, or account-link fields.

**Rationale**: The spec requires distinct access flows and prohibits self-service admin creation. The user has specified separate UI namespaces: all admin screens belong under `/admin/**`, and customer-facing registration, sign-in, and home screens belong under `/customer/**`. Existing operational customer/account management pages must therefore move under `/admin/**`; frontend route consumers and navigation must be updated consistently. The existing admin session references `AdminUser` and cannot represent a customer without coupling the two identity domains.

**Alternatives considered**:
- Reuse the admin account and role tables: rejected because it expands the admin authentication path and increases the risk of privilege confusion.
- Place customer pages under `/portal`: rejected because the required customer UI namespace is `/customer`.
- Keep existing admin operations at `/customer/**` and `/account/**`: rejected because this conflicts with the required customer UI namespace; move them into `/admin/**`.
- Link registration automatically to an existing bank customer record by email or name: rejected because the spec does not define a verified linking process and the existing `Customer` entity represents operational banking data, not login credentials.

### Collect customer details and generate username during registration

**Decision**: Require name, structured address, email, phone number, and password. Reuse the repository's address shape (`streetNumber`, `postalCode`, `city`, `province`) without creating or linking an operational bank `Customer` profile. Generate a username on the server by converting the name to lowercase, removing all whitespace (and preserving other characters), then appending a sequence beginning at `001` for each identical resulting base. The suffix is at least three digits and expands beyond `999`. For example, `Greshka Lao` generates `greshkalao001`, then `greshkalao002`. Enforce unique generated usernames and unique normalized emails with database constraints; handle concurrent registration collisions by retrying allocation inside a transaction. Return the generated username on successful registration. Email verification is out of scope; successful registration is immediately eligible for customer sign-in.

**Rationale**: The updated requirement defines the name, address, email, phone number, and password as mandatory details, and explicitly specifies username generation from the name. The existing `Address` entity provides an established structured format. Keeping the customer auth identity distinct from the existing operational `Customer` profile avoids implicitly creating banking profiles or accounts.

**Alternatives considered**:
- Ask the customer to select a username: rejected because the username is automatically generated.
- Store an unstructured address string: rejected in favor of the existing address field pattern.
- Create or link an operational `Customer` record at registration: rejected because that operational profile is distinct from the customer login identity and registration does not imply a bank account relationship.
- Require email verification: deferred because the application has no email delivery dependency and verification is not a stated requirement.

### Use Spring Security for customer authentication and authorization

**Decision**: Add `spring-boot-starter-security` to the backend and use Spring Boot's dependency management for its version. Configure a customer-only `SecurityFilterChain`, a customer `UserDetailsService`/`DaoAuthenticationProvider` with a `PasswordEncoder`, and session-backed `SecurityContext` persistence. Use `DelegatingPasswordEncoder` with an adaptive one-way encoder (bcrypt is the default selected by Spring Security's factory), tune its work factor against deployment hardware, and retain the encoded value's algorithm identifier for future upgrades. Keep CSRF enabled for customer state-changing requests and use explicit credentialed CORS for the configured frontend origin. Preserve existing behavior with a lower-priority fallback chain and tests proving admin routes and APIs have not acquired new security requirements. Apply temporary rate limits to repeated failed customer sign-in attempts without permanently locking customer accounts.

**Rationale**: This feature requires more than password hashing: it needs credential authentication, customer-principal authorization, a secure browser session, session fixation protection, and CSRF defense. Spring Security supplies established, composable support for these concerns and integrates with the existing Servlet/Spring Boot application. The original feature description said to use existing libraries only; this design deliberately revises that constraint for this security-critical capability, and Spring Boot 4.1.1 publishes a matching `spring-boot-starter-security`.

**Alternatives considered**:
- Hand-roll authentication and PBKDF2 with Java JCA: rejected because JCA does not provide authorization filters, session security, CSRF handling, or safe integration patterns.
- Keep the custom controller-only session design: rejected because it would require implementing and maintaining authentication context persistence and access enforcement directly.
- Plaintext or fast unsalted hashes: rejected as unsafe.

### Isolate Spring Security to the customer flow

**Decision**: Define ordered filter chains. The first chain matches the customer authentication API (and any future customer-only backend routes), requires customer authority on protected endpoints, permits only registration/login/CSRF bootstrap as anonymous, enables CSRF, and configures credentialed CORS for explicitly allowed origins. A lower-priority fallback chain explicitly preserves the current behavior of all non-customer endpoints rather than inheriting Spring Boot's secure-everything defaults. Add integration tests for existing admin login and API behavior after enabling the starter. Do not rely on the generated default Boot user.

**Rationale**: Spring Boot documents that when Spring Security is on the classpath, web applications are secured by default, including form login or HTTP Basic depending on request negotiation. Without explicit chains, adding the starter could unexpectedly affect existing admin and business endpoints. Spring Security supports multiple ordered chains and per-chain CORS, enabling a bounded migration.

**Alternatives considered**:
- One broad chain requiring authentication everywhere: rejected because it would alter existing admin and application API behavior.
- No fallback chain: rejected because Boot's generated default-user security behavior would be implicitly applied to endpoints outside the customer chain.
- Disable CSRF globally: rejected because customer auth uses browser cookies and state-changing requests need CSRF protection.

### Use a server-managed customer HTTP session

**Decision**: Keep customer auth state server-side in the Spring Security `SecurityContext` persisted in an HTTP session and issue the servlet session cookie with `HttpOnly`, an appropriate `SameSite` policy, and `Secure` in HTTPS deployments. Use Spring Security session-fixation protection to rotate the session identifier at authentication and expire customer sessions after 15 minutes of inactivity. Use Spring Security's CSRF token support for state-changing customer requests.

**Rationale**: The app is a browser client. Spring Security manages authentication persistence and session protection without putting bearer credentials in browser storage. A separate customer principal and customer-scoped filter chain preserve the admin boundary.

**Alternatives considered**:
- Reuse the admin local-storage bearer token: rejected because the admin auth state and logout lifecycle must remain independent, and browser storage is more exposed to script access.
- Persist a separate customer token in a JPA session table: viable if multi-instance persistence is required, but not necessary for the current single-service feature; session clustering/shared storage can be added when deployment requires it.
- Return a session token to JavaScript: rejected for the browser use case in favor of an `HttpOnly` cookie.
- Continue with a manual `HttpSession` and hand-written CSRF/session logic: rejected in favor of Spring Security's established security context, CSRF, and session management.

### Use customer-specific routes and accessible form feedback

**Decision**: Move admin sign-in and all admin pages under `/admin/**` (including `/admin/login`, `/admin/dashboard`, `/admin/customers/**`, and `/admin/accounts/**`). Use `/customer/login`, `/customer/register`, and `/customer/home` for customer access, with a public `/` entry page. Use a distinct customer auth context/client and customer-only route guard, plus an admin-only guard for `/admin/**`. Update navigation and links to the new canonical paths. Provide visible form labels, browser autocomplete semantics, field validation, and clear accessible error/success feedback.

**Rationale**: This fits the existing React Router declarative routes without adding dependencies. Role-specific guards and independent customer session calls prevent a customer identity from satisfying an admin route check.

**Alternatives considered**:
- Reuse the admin `AuthContext` or `ProtectedRoute` without principal-type checks: rejected because the existing guard checks only for a user, not the user class.
- Replace React Router or introduce a data-router migration: unnecessary for this feature.

## Findings

- The backend is Java 17 / Spring Boot 4.1.1 and already uses Spring Data JPA, Spring Web MVC, and MySQL Connector/J (`backend/pom.xml`).
- The configured servlet context path is `/sprint4Deliverable`; the existing admin API is `/api/auth/**`.
- The existing frontend uses React Router declarative routes. Existing admin routes include `/login`, `/dashboard`, `/customer/**`, and `/account/**`. Reassign all of them beneath `/admin/**`; customer-facing routes use `/customer/**`.
- The current admin auth uses `AdminUser`, the admin `Session` entity, and a browser token stored in local storage. The existing session association is admin-specific.
- The current `Customer` entity is an abstract, joined-inheritance operational profile with bank account relationships. It is not an authentication identity.
- The project has no active constitution rules: `.specify/memory/constitution.md` is an unfilled template.
- The original POM did not include Spring Security. Spring Boot 4.1.1 publishes `spring-boot-starter-security` and manages its transitive versions. Existing admin routes use `/api/auth/**`, and the existing admin session is associated with `AdminUser`.
- Spring Security integrates with Servlet applications through filters, supports ordered `SecurityFilterChain` instances, adaptive password encoders, CSRF protection, session management, and MVC-aware or per-chain CORS.

## References

- Feature requirements and boundaries: [spec.md](./spec.md)
- Existing backend dependency/runtime configuration: `backend/pom.xml`, `backend/src/main/resources/application.properties`
- Existing route and identity patterns: `frontend/src/App.jsx`, `frontend/src/context/AuthContext.jsx`, `frontend/src/components/ProtectedRoute.jsx`, `backend/src/main/java/com/fdmgroup/sprint4Deliverable/Model/Session.java`
- OWASP Password Storage Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- OWASP Session Management Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
- OWASP CSRF Prevention Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html
- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- Spring Boot 4.1 Spring Security behavior and defaults: https://docs.spring.io/spring-boot/reference/web/spring-security.html
- Spring Security password storage and `PasswordEncoder`: https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
- Spring Security `DaoAuthenticationProvider`: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/dao-authentication-provider.html
- Spring Security session management and authentication persistence: https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html
- Spring Security CSRF protection: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- Spring Security CORS integration and per-chain configuration: https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html
- Spring Boot 4.1.1 `spring-boot-starter-security` artifact: https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-security/4.1.1/spring-boot-starter-security-4.1.1.pom
- W3C WAI form labels and notifications: https://www.w3.org/WAI/tutorials/forms/labels/ and https://www.w3.org/WAI/tutorials/forms/notifications/

## Resolved Open Questions

- **Registration fields**: name, the current address fields (street number, postal code, city, province), email, E.164 phone number, and password; generate and return the username from normalized name with an incrementing suffix. No operational bank-customer or account record is created or linked.
- **Password policy**: minimum 8 characters, support at least 64 characters, no composition rules, as selected in clarification. This is shorter than OWASP's general password-only recommendation of 15 characters and should be treated as an explicit product decision.
- **Failed sign-in behavior**: temporarily rate-limit repeated failures without permanently locking accounts.
- **Customer session timeout**: 15 minutes of inactivity.
- **Email verification**: not included; a valid-format email is collected but is not verified.
- **Session mechanism**: Spring Security `SecurityContext` in a server-managed cookie-backed HTTP session, separate from admin auth.
- **Route naming**: `/admin/**` for every admin UI route and `/customer/**` for customer registration, sign-in, and home; update existing admin navigation and route references during the move.
- **Password storage**: Spring Security `DelegatingPasswordEncoder` with an adaptive encoder and deployment-tuned work factor.
- **Dependency constraint**: Spring Security is a focused exception to the original existing-libraries-only constraint; its established auth/session/CSRF/authorization controls are safer than hand-rolled equivalents.
