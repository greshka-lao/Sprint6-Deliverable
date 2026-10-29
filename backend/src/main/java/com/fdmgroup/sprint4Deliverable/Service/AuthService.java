package com.fdmgroup.sprint4Deliverable.Service;

import java.time.Instant;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fdmgroup.sprint4Deliverable.Exception.AdminAuthenticationException;
import com.fdmgroup.sprint4Deliverable.Exception.ValidationException;
import com.fdmgroup.sprint4Deliverable.Model.AccessAttempt;
import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.AuthResponse;
import com.fdmgroup.sprint4Deliverable.Model.Session;
import com.fdmgroup.sprint4Deliverable.Repository.AccessAttemptRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AdminUserRepository;
import com.fdmgroup.sprint4Deliverable.Repository.SessionRepository;
import com.fdmgroup.sprint4Deliverable.Security.TokenDigest;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long SESSION_DURATION_SECONDS = 15L * 60L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String INVALID_CREDENTIALS = "Invalid username or password.";
    private static final String LOCKED_MESSAGE =
            "This account is temporarily locked or inactive. Contact an administrator.";
    private static final String EXPIRED_MESSAGE = "Your session has expired. Please sign in again.";

    private final AdminUserRepository adminUserRepository;
    private final SessionRepository sessionRepository;
    private final AccessAttemptRepository accessAttemptRepository;
    private final AuthorizationService authorizationService;
    private final AuthenticationManager authenticationManager;
    private final AdminSecurityEventService securityEventService;

    public AuthService(
            AdminUserRepository adminUserRepository,
            SessionRepository sessionRepository,
            AccessAttemptRepository accessAttemptRepository,
            AuthorizationService authorizationService,
            @Qualifier("adminAuthenticationManager")
            AuthenticationManager authenticationManager,
            AdminSecurityEventService securityEventService) {
        this.adminUserRepository = adminUserRepository;
        this.sessionRepository = sessionRepository;
        this.accessAttemptRepository = accessAttemptRepository;
        this.authorizationService = authorizationService;
        this.authenticationManager = authenticationManager;
        this.securityEventService = securityEventService;
    }

    @Transactional(noRollbackFor = {ValidationException.class, AdminAuthenticationException.class})
    public AuthResponse login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Username and password are required.");
        }

        String loginIdentifier = username.trim();
        Optional<AdminUser> optionalUser = adminUserRepository
                .findByUsernameOrEmail(loginIdentifier, loginIdentifier);
        if (optionalUser.isEmpty()) {
            recordFailure(loginIdentifier);
            throw authenticationFailure();
        }

        AdminUser adminUser = optionalUser.get();
        Instant now = Instant.now();
        if (!adminUser.isEnabled()) {
            recordFailure(loginIdentifier);
            throw lockedFailure();
        }
        if (isLocked(adminUser, now)) {
            recordFailure(loginIdentifier);
            throw lockedFailure();
        }

        if (adminUser.getLockedUntil() != null) {
            adminUser.setLockedUntil(null);
            adminUser.setFailedAttempts(0);
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginIdentifier, password));
        } catch (BadCredentialsException exception) {
            recordFailure(loginIdentifier);
            adminUser.setFailedAttempts(adminUser.getFailedAttempts() + 1);
            if (adminUser.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                adminUser.setLockedUntil(now.plusSeconds(SESSION_DURATION_SECONDS));
                adminUserRepository.save(adminUser);
                securityEventService.record(adminUser, "LOCKOUT", "TEMPORARY", adminUser.getUsername());
                throw lockedFailure();
            }
            adminUserRepository.save(adminUser);
            throw authenticationFailure();
        } catch (AccountStatusException exception) {
            recordFailure(loginIdentifier);
            throw lockedFailure();
        }

        adminUser.setFailedAttempts(0);
        adminUser.setLockedUntil(null);
        adminUser.setLastLoginAt(now);
        adminUserRepository.save(adminUser);

        Optional<Session> activeSession = sessionRepository.findByAdminUserAndActiveTrue(adminUser);
        if (activeSession.isPresent()) {
            Session existing = activeSession.get();
            existing.setActive(false);
            sessionRepository.save(existing);
        }

        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Session session = new Session();
        session.setTokenDigest(TokenDigest.sha256(token));
        session.setAdminUser(adminUser);
        session.setActive(true);
        session.setLastUsedAt(now);
        session.setExpiresAt(now.plusSeconds(SESSION_DURATION_SECONDS));
        sessionRepository.save(session);

        recordSuccess(loginIdentifier);
        securityEventService.record(adminUser, "SIGN_IN", "SUCCESS", adminUser.getUsername());
        return new AuthResponse(token, adminUser.getUsername(), adminUser.getRole().getName(), "Login successful");
    }

    @Transactional(noRollbackFor = AdminAuthenticationException.class)
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        sessionRepository.findByTokenDigest(TokenDigest.sha256(token)).ifPresent(session -> {
            session.setActive(false);
            sessionRepository.save(session);
            securityEventService.record(session.getAdminUser(), "SIGN_OUT", "SUCCESS",
                    session.getAdminUser().getUsername());
        });
    }

    @Transactional(noRollbackFor = AdminAuthenticationException.class)
    public AdminUser validateSession(String token) {
        if (token == null || token.isBlank()) {
            throw expiredFailure();
        }

        Optional<AdminUser> user = resolveToken(token, true);
        if (user.isEmpty()) {
            throw expiredFailure();
        }
        return user.get();
    }

    @Transactional(noRollbackFor = AdminAuthenticationException.class)
    public Optional<AdminUser> authenticateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return resolveToken(token, false);
    }

    public boolean isAdminUser(AdminUser adminUser) {
        return authorizationService.hasAdminAccess(adminUser);
    }

    private Optional<AdminUser> resolveToken(String token, boolean requireAdminRole) {
        Optional<Session> sessionOptional = sessionRepository.findByTokenDigest(TokenDigest.sha256(token));
        if (sessionOptional.isEmpty()) {
            return Optional.empty();
        }

        Session session = sessionOptional.get();
        Instant now = Instant.now();
        if (!session.isActive() || !session.getExpiresAt().isAfter(now)) {
            if (session.isActive()) {
                session.setActive(false);
                sessionRepository.save(session);
                securityEventService.record(session.getAdminUser(), "SESSION_EXPIRY", "EXPIRED",
                        session.getAdminUser().getUsername());
            }
            return Optional.empty();
        }

        AdminUser user = session.getAdminUser();
        if (user == null || !user.isEnabled()) {
            session.setActive(false);
            sessionRepository.save(session);
            securityEventService.record(user, "ACCESS_DENIED", "INELIGIBLE_ACCOUNT",
                    user == null ? null : user.getUsername());
            return Optional.empty();
        }
        if (requireAdminRole && !authorizationService.hasAdminAccess(user)) {
            securityEventService.record(user, "ACCESS_DENIED", "INELIGIBLE_ROLE", user.getUsername());
            return Optional.empty();
        }

        session.setLastUsedAt(now);
        session.setExpiresAt(now.plusSeconds(SESSION_DURATION_SECONDS));
        sessionRepository.save(session);
        return Optional.of(user);
    }

    private boolean isLocked(AdminUser adminUser, Instant now) {
        return adminUser.getLockedUntil() != null && adminUser.getLockedUntil().isAfter(now);
    }

    private void recordFailure(String username) {
        accessAttemptRepository.save(new AccessAttempt(username, false));
    }

    private void recordSuccess(String username) {
        accessAttemptRepository.save(new AccessAttempt(username, true));
    }

    private AdminAuthenticationException authenticationFailure() {
        return new AdminAuthenticationException(HttpStatus.UNAUTHORIZED, "authentication_failed",
                INVALID_CREDENTIALS);
    }

    private AdminAuthenticationException lockedFailure() {
        return new AdminAuthenticationException(HttpStatus.LOCKED, "account_locked", LOCKED_MESSAGE);
    }

    private AdminAuthenticationException expiredFailure() {
        return new AdminAuthenticationException(HttpStatus.UNAUTHORIZED, "session_expired", EXPIRED_MESSAGE);
    }
}
