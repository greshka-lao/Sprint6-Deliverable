package com.fdmgroup.sprint4Deliverable.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.dao.DataAccessResourceFailureException;

import com.fdmgroup.sprint4Deliverable.Exception.AdminAuthenticationException;
import com.fdmgroup.sprint4Deliverable.Exception.ValidationException;
import com.fdmgroup.sprint4Deliverable.Model.AccessAttempt;
import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.AuthResponse;
import com.fdmgroup.sprint4Deliverable.Model.Role;
import com.fdmgroup.sprint4Deliverable.Model.Session;
import com.fdmgroup.sprint4Deliverable.Repository.AccessAttemptRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AdminUserRepository;
import com.fdmgroup.sprint4Deliverable.Repository.SessionRepository;
import com.fdmgroup.sprint4Deliverable.Security.TokenDigest;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private AccessAttemptRepository accessAttemptRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AdminSecurityEventService securityEventService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(adminUserRepository, sessionRepository, accessAttemptRepository,
                new AuthorizationService(), authenticationManager, securityEventService);
    }

    @Test
    void loginSuccessReturnsHighEntropyTokenAndPersistsOnlyItsDigest() {
        AdminUser adminUser = adminUser("bank.teller", "BANK_TELLER", "password123");
        when(adminUserRepository.findByUsernameOrEmail("bank.teller", "bank.teller"))
                .thenReturn(Optional.of(adminUser));
        when(sessionRepository.findByAdminUserAndActiveTrue(adminUser)).thenReturn(Optional.empty());
        Instant beforeLogin = Instant.now();

        AuthResponse response = authService.login("bank.teller", "password123");
        Instant afterLogin = Instant.now();

        assertNotNull(response.getToken());
        assertEquals(43, response.getToken().length());
        assertEquals("bank.teller", response.getUsername());
        assertEquals("BANK_TELLER", response.getRole());
        org.mockito.ArgumentCaptor<Session> sessionCaptor = org.mockito.ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertEquals(TokenDigest.sha256(response.getToken()), sessionCaptor.getValue().getTokenDigest());
        assertFalse(response.getToken().equals(sessionCaptor.getValue().getTokenDigest()));
        assertTrue(!sessionCaptor.getValue().getExpiresAt().isBefore(beforeLogin.plusSeconds(900)));
        assertTrue(!sessionCaptor.getValue().getExpiresAt().isAfter(afterLogin.plusSeconds(900)));
        verify(accessAttemptRepository).save(any(AccessAttempt.class));
    }

    @Test
    void loginByEmailAuthenticatesCanonicalAdminAccount() {
        AdminUser adminUser = adminUser("bank.teller", "BANK_TELLER", "password123");
        when(adminUserRepository.findByUsernameOrEmail(
                "bank.teller@example.test", "bank.teller@example.test")).thenReturn(Optional.of(adminUser));
        when(sessionRepository.findByAdminUserAndActiveTrue(adminUser)).thenReturn(Optional.empty());

        AuthResponse response = authService.login("bank.teller@example.test", "password123");

        assertEquals("bank.teller", response.getUsername());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void unknownAndIncorrectCredentialsUseEquivalentPublicFailures() {
        AdminUser adminUser = adminUser("bank.teller", "BANK_TELLER", "password123");
        when(adminUserRepository.findByUsernameOrEmail("missing", "missing")).thenReturn(Optional.empty());
        when(adminUserRepository.findByUsernameOrEmail("bank.teller", "bank.teller"))
                .thenReturn(Optional.of(adminUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AdminAuthenticationException unknown = assertThrows(AdminAuthenticationException.class,
                () -> authService.login("missing", "wrong"));
        AdminAuthenticationException incorrect = assertThrows(AdminAuthenticationException.class,
                () -> authService.login("bank.teller", "wrong"));

        assertEquals(HttpStatus.UNAUTHORIZED, unknown.getStatus());
        assertEquals(unknown.getErrorCode(), incorrect.getErrorCode());
        assertEquals(unknown.getMessage(), incorrect.getMessage());
    }

    @Test
    void fifthFailedAttemptLocksWithoutDisablingAccount() {
        AdminUser adminUser = adminUser("bank.teller", "BANK_TELLER", "password123");
        adminUser.setFailedAttempts(4);
        when(adminUserRepository.findByUsernameOrEmail("bank.teller", "bank.teller"))
                .thenReturn(Optional.of(adminUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        AdminAuthenticationException exception = assertThrows(AdminAuthenticationException.class,
                () -> authService.login("bank.teller", "wrong"));

        assertEquals(HttpStatus.LOCKED, exception.getStatus());
        assertEquals(5, adminUser.getFailedAttempts());
        assertNotNull(adminUser.getLockedUntil());
        assertEquals(true, adminUser.isEnabled());
        verify(securityEventService).record(adminUser, "LOCKOUT", "TEMPORARY", "bank.teller");
    }

    @Test
    void expiredTemporaryLockResetsBeforeSuccessfulAuthentication() {
        AdminUser adminUser = adminUser("bank.teller", "BANK_TELLER", "password123");
        adminUser.setFailedAttempts(5);
        adminUser.setLockedUntil(Instant.now().minusSeconds(1));
        when(adminUserRepository.findByUsernameOrEmail("bank.teller", "bank.teller"))
                .thenReturn(Optional.of(adminUser));
        when(sessionRepository.findByAdminUserAndActiveTrue(adminUser)).thenReturn(Optional.empty());

        authService.login("bank.teller", "password123");

        assertEquals(0, adminUser.getFailedAttempts());
        assertNull(adminUser.getLockedUntil());
    }

    @Test
    void passwordUpgradePersistenceFailureDoesNotCreateSession() {
        AdminUser adminUser = adminUser("bank.teller", "BANK_TELLER", "password123");
        when(adminUserRepository.findByUsernameOrEmail("bank.teller", "bank.teller"))
                .thenReturn(Optional.of(adminUser));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DataAccessResourceFailureException("password update failed"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> authService.login("bank.teller", "password123"));

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void successfulLoginReplacesPreviousActiveSession() {
        AdminUser adminUser = adminUser("bank.teller", "BANK_TELLER", "password123");
        Session oldSession = new Session();
        oldSession.setActive(true);
        when(adminUserRepository.findByUsernameOrEmail("bank.teller", "bank.teller"))
                .thenReturn(Optional.of(adminUser));
        when(sessionRepository.findByAdminUserAndActiveTrue(adminUser)).thenReturn(Optional.of(oldSession));

        AuthResponse response = authService.login("bank.teller", "password123");

        assertFalse(oldSession.isActive());
        verify(sessionRepository).save(oldSession);
        verify(sessionRepository, org.mockito.Mockito.times(2)).save(any(Session.class));
        assertNotNull(response.getToken());
    }

    @Test
    void logoutRevokesDigestMatchedSessionAndRecordsEvent() {
        AdminUser adminUser = adminUser("bank.teller", "BANK_TELLER", "password123");
        Session session = new Session();
        session.setAdminUser(adminUser);
        session.setActive(true);
        when(sessionRepository.findByTokenDigest(TokenDigest.sha256("raw-token")))
                .thenReturn(Optional.of(session));

        authService.logout("raw-token");

        assertFalse(session.isActive());
        verify(sessionRepository).save(session);
        verify(securityEventService).record(adminUser, "SIGN_OUT", "SUCCESS", "bank.teller");
    }

    @Test
    void missingCredentialsDoNotAuthenticate() {
        assertThrows(ValidationException.class, () -> authService.login(" ", "password"));
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void expiredSessionIsRevokedAndRejected() {
        AdminUser adminUser = adminUser("bank.manager", "BANK_MANAGER", "password123");
        Session session = new Session();
        session.setTokenDigest(TokenDigest.sha256("expired-token"));
        session.setAdminUser(adminUser);
        session.setActive(true);
        session.setExpiresAt(Instant.now().minusSeconds(60));
        when(sessionRepository.findByTokenDigest(TokenDigest.sha256("expired-token")))
                .thenReturn(Optional.of(session));

        AdminAuthenticationException exception = assertThrows(AdminAuthenticationException.class,
                () -> authService.validateSession("expired-token"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
        assertFalse(session.isActive());
        verify(sessionRepository).save(session);
        verify(securityEventService).record(adminUser, "SESSION_EXPIRY", "EXPIRED", "bank.manager");
    }

    private AdminUser adminUser(String username, String role, String password) {
        return new AdminUser(username, username + "@example.test", password, new Role(role, role));
    }
}
