package com.fdmgroup.sprint4Deliverable.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fdmgroup.sprint4Deliverable.Exception.ValidationException;
import com.fdmgroup.sprint4Deliverable.Model.AccessAttempt;
import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.AuthResponse;
import com.fdmgroup.sprint4Deliverable.Model.Session;
import com.fdmgroup.sprint4Deliverable.Repository.AccessAttemptRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AdminUserRepository;
import com.fdmgroup.sprint4Deliverable.Repository.SessionRepository;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long SESSION_DURATION_SECONDS = 15L * 60L;

    private final AdminUserRepository adminUserRepository;
    private final SessionRepository sessionRepository;
    private final AccessAttemptRepository accessAttemptRepository;
    private final AuthorizationService authorizationService;

    public AuthService(
            AdminUserRepository adminUserRepository,
            SessionRepository sessionRepository,
            AccessAttemptRepository accessAttemptRepository,
            AuthorizationService authorizationService) {
        this.adminUserRepository = adminUserRepository;
        this.sessionRepository = sessionRepository;
        this.accessAttemptRepository = accessAttemptRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ValidationException("Username and password are required.");
        }

        Optional<AdminUser> optionalUser = adminUserRepository.findByUsernameOrEmail(username.trim(), username.trim());
        if (optionalUser.isEmpty()) {
            recordFailure(username);
            throw new ValidationException("Invalid username or password.");
        }

        AdminUser adminUser = optionalUser.get();
        if (!adminUser.isEnabled() || isLocked(adminUser)) {
            recordFailure(username);
            throw new ValidationException("This account is temporarily locked or inactive. Contact an administrator.");
        }

        if (!adminUser.getPassword().equals(password)) {
            recordFailure(username);
            adminUser.setFailedAttempts(adminUser.getFailedAttempts() + 1);
            if (adminUser.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                adminUser.setEnabled(false);
                adminUser.setLockedUntil(Instant.now().plusSeconds(900));
            }
            adminUserRepository.save(adminUser);
            throw new ValidationException("Invalid username or password.");
        }

        adminUser.setFailedAttempts(0);
        adminUser.setLastLoginAt(Instant.now());
        adminUserRepository.save(adminUser);

        Optional<Session> activeSession = sessionRepository.findByAdminUserAndActiveTrue(adminUser);
        if (activeSession.isPresent()) {
            Session existing = activeSession.get();
            existing.setActive(false);
            sessionRepository.save(existing);
        }

        String token = UUID.randomUUID().toString();
        Session session = new Session();
        session.setToken(token);
        session.setAdminUser(adminUser);
        session.setActive(true);
        session.setExpiresAt(Instant.now().plusSeconds(SESSION_DURATION_SECONDS));
        sessionRepository.save(session);

        recordSuccess(username);
        return new AuthResponse(token, adminUser.getUsername(), adminUser.getRole().getName(), "Login successful");
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        sessionRepository.findByToken(token).ifPresent(session -> {
            session.setActive(false);
            sessionRepository.save(session);
        });
    }

    @Transactional(readOnly = true)
    public AdminUser validateSession(String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Your session has expired. Please sign in again.");
        }

        Optional<Session> sessionOptional = sessionRepository.findByToken(token);
        if (sessionOptional.isEmpty() || !sessionOptional.get().isActive()) {
            throw new ValidationException("Your session has expired. Please sign in again.");
        }

        Session session = sessionOptional.get();
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setActive(false);
            sessionRepository.save(session);
            throw new ValidationException("Your session has expired. Please sign in again.");
        }

        session.setLastUsedAt(Instant.now());
        sessionRepository.save(session);

        AdminUser user = session.getAdminUser();
        if (user == null || !authorizationService.hasAdminAccess(user)) {
            throw new ValidationException("You do not have permission to access this page.");
        }
        return user;
    }

    public boolean isAdminUser(AdminUser adminUser) {
        return authorizationService.hasAdminAccess(adminUser);
    }

    private boolean isLocked(AdminUser adminUser) {
        return adminUser.getLockedUntil() != null && adminUser.getLockedUntil().isAfter(Instant.now());
    }

    private void recordFailure(String username) {
        accessAttemptRepository.save(new AccessAttempt(username, false));
    }

    private void recordSuccess(String username) {
        accessAttemptRepository.save(new AccessAttempt(username, true));
    }
}
