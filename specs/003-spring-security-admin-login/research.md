# Research: Spring Security for Admin Login

## Decisions

### Integrate framework authentication with the existing JSON login API

**Decision**: Keep `POST /api/auth/login` as the entry point, but delegate credential verification to an explicitly admin-scoped Spring Security `AuthenticationManager` using a `DaoAuthenticationProvider`. Preserve the existing opaque bearer token, response fields, frontend `Authorization` header, and database-backed session lifecycle.

**Rationale**: The current client stores `token`, `username`, and `role` and sends the token in `Authorization`; changing this to a servlet cookie session would break client behavior and blur the customer/admin separation. Spring Security supports publishing an `AuthenticationManager` for REST authentication. An API request filter can adapt existing opaque tokens into a Spring Security principal on each protected request while keeping the database session as the revocation and inactivity source of truth.

**Alternatives considered**:

- Replace the admin API token with an HTTP session cookie: rejected for this feature because it breaks the existing admin API/client contract and risks conflating it with the customer session chain.
- Continue validating tokens manually in each controller: rejected because endpoint-by-endpoint checks are easy to omit and do not provide centralized Spring Security authorization.
- Issue JWTs: rejected because a signed self-contained token complicates immediate revocation, single-active-session behavior, and idle-timeout enforcement; there is no requirement to replace the current server-side session record.

**Sources**:

- [Spring Security: Publish an AuthenticationManager](https://docs.spring.io/spring-security/reference/7.1/servlet/authentication/passwords/index.html#publish-authentication-manager-bean)
- [Spring Security: SecurityFilterChain configuration](https://docs.spring.io/spring-security/reference/7.1/servlet/configuration/java.html)

### Upgrade legacy passwords only after successful authentication

**Decision**: Configure the admin DAO provider with a legacy-aware password encoder and `UserDetailsPasswordService`. After a successful credential comparison, replace a legacy stored credential with the current delegating adaptive hash before the login flow issues a session token. If saving the upgraded credential fails, do not complete sign-in or create a session.

**Rationale**: Spring Security's `DaoAuthenticationProvider` supports password upgrades after successful authentication when the encoder requests an upgrade and a `UserDetailsPasswordService` is configured. The existing seed accounts contain unprefixed plaintext development passwords, so the legacy comparison must be explicit; the delegating encoder alone must not be assumed to accept unprefixed credentials. New credentials and migrated values must use a supported algorithm prefix and adaptive encoder.

**Alternatives considered**:

- Force every existing administrator to reset before using the system: rejected because the specification requires first-success migration and existing account continuity.
- Continue storing plaintext passwords or migrate before verification: rejected because this preserves or increases exposure of recoverable secrets.

**Sources**:

- [Spring Security: Password Storage](https://docs.spring.io/spring-security/reference/7.1/features/authentication/password-storage.html)
- [Spring Security: UserDetailsPasswordService API](https://docs.spring.io/spring-security/reference/7.1/api/java/org/springframework/security/core/userdetails/UserDetailsPasswordService.html)
- [DaoAuthenticationProvider password-upgrade behavior](https://github.com/spring-projects/spring-security/blob/7.1.x/core/src/main/java/org/springframework/security/authentication/dao/DaoAuthenticationProvider.java#L135-L143)
- [PasswordEncoder API](https://docs.spring.io/spring-security/reference/7.1/api/java/org/springframework/security/crypto/password/PasswordEncoder.html)

### Order customer, admin, and compatibility security chains

**Decision**: Retain the customer-authentication chain as the highest-priority chain for `/api/customer-auth/**`; add an admin chain for `/api/auth/**`, `/customer/**`, and `/account/**` after it; leave a final compatibility chain for unrelated legacy routes. The admin chain uses bearer-token request authentication and does not restore its principal from an `HttpSession`.

**Rationale**: Spring Security evaluates filter chains in order, and only the first matching chain applies. Explicit scopes prevent the permissive compatibility chain from bypassing admin authorization and preserve the customer's separate cookie/CSRF flow. Separate matchers alone do not guarantee separate identities if both chains share the same session-backed security context; avoiding servlet-session authentication in the admin chain enforces the intended identity separation.

**Alternatives considered**:

- A single shared chain/session for both customer and admin identities: rejected because it can share authentication context and complicates authorization and logout isolation.
- Put the permissive compatibility chain before admin security: rejected because earlier matchers would capture and bypass protected requests.
- Make all routes require admin authentication: rejected because unrelated legacy/public routes are outside this feature and must retain existing behavior.

**Sources**:

- [Spring Security: Multiple `SecurityFilterChain` instances](https://docs.spring.io/spring-security/reference/7.1/servlet/configuration/java.html)
- [Spring Security: Customizing where authentication is stored](https://docs.spring.io/spring-security/reference/7.1/servlet/authentication/session-management.html#customizing-where-authentication-is-stored)

### Keep administrator bearer-token requests separate from customer CSRF sessions

**Decision**: Keep CSRF enabled and session-backed for the customer chain. The admin client authenticates operational requests using an explicitly attached `Authorization` bearer header and not an automatically sent cookie; keep the admin chain independent of servlet `HttpSession`, restrict CORS origins, and do not configure credentialed wildcard origins.

**Rationale**: CSRF protections are essential for browser authentication that relies on ambient session cookies. The existing admin token is manually attached by the client, so it is not automatically sent cross-site like a cookie. Preserving separate policies avoids weakening the customer session chain. If the admin client later moves to cookie authentication, CSRF requirements must be revisited rather than silently inherited from the bearer-token design.

**Alternatives considered**:

- Disable CSRF globally: rejected because the customer session chain relies on CSRF protection.
- Move admin bearer token to a cookie without adding CSRF protection: rejected because browsers attach cookies automatically to cross-site requests.
- Share the customer session repository: rejected because logout or session restoration could cross identity boundaries.

**Sources**:

- [Spring Security: Cross Site Request Forgery](https://docs.spring.io/spring-security/reference/7.1/servlet/exploits/csrf.html)
- [Spring Security: Session management and context storage](https://docs.spring.io/spring-security/reference/7.1/servlet/authentication/session-management.html)

### Treat idle expiry and temporary account lock separately from administrative disablement

**Decision**: Continue using the persisted `Session` record for administrator tokens and enforce a rolling 15-minute inactivity window by updating its expiry on successful validation. After five consecutive failures, set a 15-minute temporary lock and clear the lock and failure count when it expires. Keep administrator `enabled` state independent so an administratively disabled account remains disabled.

**Rationale**: The existing `Session` entity and repository already support token revocation and expiry, while `AdminUser` includes both `enabled`, `failedAttempts`, and `lockedUntil`. The current login flow sets `enabled=false` at the failure threshold, which prevents the stated automatic unlock behavior; the new lock state must not be conflated with permanent administrative disablement. A standard servlet session timeout does not control persisted bearer tokens, so expiry must remain enforced by the token/session service.

**Alternatives considered**:

- Use only the servlet session timeout: rejected because it would not expire the database-backed bearer token used by the admin client.
- Keep account `enabled=false` for temporary lockout: rejected because automatic lock expiry would not safely distinguish a disabled account.
- Replace persisted opaque tokens with JWTs: rejected because server-side revocation and idle expiry are already supported by the current entity model.

**Sources**:

- [Spring Boot server servlet session timeout property](https://docs.spring.io/spring-boot/4.0/appendix/application-properties/index.html#application-properties.server.server.servlet.session.timeout)
- [Spring Security session management](https://docs.spring.io/spring-security/reference/7.1/servlet/authentication/session-management.html)

### Return bearer tokens only at login and store only a digest

**Decision**: Return the high-entropy opaque bearer token only in the successful login response, keep the existing token field null on later session-validation responses, and persist a one-way digest of the token for lookup and revocation.

**Rationale**: The successful login response is required by the existing client, but repeating the secret on session checks unnecessarily increases exposure. A digest limits the impact of session-table disclosure; because the token is randomly generated with high entropy, a fast one-way digest is sufficient for lookup and does not need password-hashing semantics.

**Alternatives considered**:

- Store raw tokens in the database: rejected because a database disclosure would immediately expose active bearer credentials.
- Echo the token on each session validation: rejected because the client already has the token and the feature clarifies that the secret is returned only at login.
- Switch to cookie-based sessions: rejected for this feature because the existing API/client contract explicitly keeps a bearer token in the successful login response.

### Preserve administrator data and confine development identities

**Decision**: Stop destructive schema recreation for the administrator store, preserve database rows across restarts, and run the seeded teller/manager accounts only under an explicit development profile. Production startup must not create known default administrator credentials.

**Rationale**: The current `spring.jpa.hibernate.ddl-auto=create` setting drops and recreates the schema on application startup, which defeats first-success credential upgrades and invalidates persisted sessions. The existing seeder creates predictable plaintext development credentials, so it must not run in production. For local development, a non-destructive schema update is sufficient for the feature's additive changes; production deployment should use a managed database migration process.

**Alternatives considered**:

- Keep schema recreation: rejected because it deletes account and session state.
- Keep default users active in every profile: rejected because predictable credentials would create a production access vulnerability.
- Add a migration framework solely for this feature: rejected at this planning stage because no migration tool is present; production migration management remains an operational deployment requirement.

## Repository Findings

- The project uses Java 17 and Spring Boot 4.1.1; `spring-boot-starter-security` is already declared in `backend/pom.xml`.
- `CustomerSecurityConfig` has an ordered customer-auth chain for `/api/customer-auth/**` and a permissive fallback chain. The admin chain must be ordered ahead of the fallback, while leaving the customer chain isolated.
- `AuthController` exposes `POST /api/auth/login`, `POST /api/auth/logout`, and `GET /api/auth/session`. `AuthResponse` currently serializes `token`, `username`, `role`, and `message`.
- The existing React admin auth context stores the token and sends it as an `Authorization` header. The API contract example in `specs/001-admin-login/contracts/authentication-api.md` does not match the current runtime DTO and must be reconciled in this feature's contract.
- The current `AuthController` also echoes the raw token from `GET /api/auth/session`; the new contract should keep the existing token field but set it to `null` to satisfy the clarified rule that the token is returned only on successful login. The frontend validates username/role and uses its already-stored token, so it does not depend on the response echo.
- `AuthService` uses `AdminUserRepository`, `SessionRepository`, and `AccessAttemptRepository`; `Session` stores one opaque token, its owner, active flag, and timestamps.
- `DataSeeder` creates development teller and manager accounts with plaintext passwords. These credentials must only be accepted through the explicit legacy upgrade path; the plan does not treat the seed credentials as production identities.
- `CustomerSecurityConfig` has its own `PasswordEncoder` and `DaoAuthenticationProvider`; admin authentication must be explicitly scoped to prevent customer lookups or shared principals.
- Customer and administrator authentication use separately qualified `ProviderManager` instances. The admin service is explicitly injected with the administrator manager; the customer chain and service retain their own manager and password encoder.
- `AuthService` currently disables the account at five failed attempts even though it also records `lockedUntil`; this must be changed to separate temporary lockout from administrator disablement.
- `application.properties` currently configures destructive Hibernate schema recreation, and `DataSeeder` inserts known plaintext development credentials. The implementation must preserve account data and keep seeded credentials confined to an explicit development profile.
- Existing session tokens are stored in the `token` database column. Reuse that column for SHA-256 digests and migrate legacy raw values in place to avoid both destructive schema changes and continued storage of reusable secrets.
- The backend test suite includes `AuthServiceTests`; new tests need to verify credential upgrade, failure to persist upgrade, lockout expiry, filter-chain separation, protected endpoints, and admin API compatibility.

## Open Design Constraints

- Keep all application endpoints and response fields compatible with the current admin frontend. Any intentional error-status change must retain the existing safe `error`/`message` JSON fields and be covered by integration tests.
- Audit requirements include events beyond login attempts; use a narrowly scoped security-event record for logout, lockout, expiry, and denied access while retaining `AccessAttempt` for login history.
- The legacy-aware encoder must not treat arbitrary unprefixed values as an indefinitely valid password format: accept only the documented legacy condition, then require a successful upgrade before token creation.
