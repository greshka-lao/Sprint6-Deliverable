package com.fdmgroup.sprint4Deliverable.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fdmgroup.sprint4Deliverable.Exception.ValidationException;
import com.fdmgroup.sprint4Deliverable.Model.AccessAttempt;
import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.AuthResponse;
import com.fdmgroup.sprint4Deliverable.Model.Role;
import com.fdmgroup.sprint4Deliverable.Model.Session;
import com.fdmgroup.sprint4Deliverable.Repository.AccessAttemptRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AdminUserRepository;
import com.fdmgroup.sprint4Deliverable.Repository.SessionRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private AccessAttemptRepository accessAttemptRepository;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(adminUserRepository, sessionRepository, accessAttemptRepository, new AuthorizationService());
    }

    @Test
    void login_success_returnsTokenAndUser() {
        Role role = new Role("BANK_TELLER", "Bank teller");
        AdminUser user = new AdminUser("bank.teller", "bank.teller@hooperbank.local", "password123", role);

        when(adminUserRepository.findByUsernameOrEmail("bank.teller", "bank.teller")).thenReturn(Optional.of(user));
        when(sessionRepository.findByAdminUserAndActiveTrue(user)).thenReturn(Optional.empty());

        AuthResponse response = authService.login("bank.teller", "password123");

        assertNotNull(response.getToken());
        assertEquals("bank.teller", response.getUsername());
        assertEquals("BANK_TELLER", response.getRole());
        verify(accessAttemptRepository).save(any(AccessAttempt.class));
    }

    @Test
    void login_invalidCredentials_throwsValidationException() {
        when(adminUserRepository.findByUsernameOrEmail("bank.teller", "bank.teller")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> authService.login("bank.teller", "wrong"));
    }

    @Test
    void validateSession_expiredSession_throwsValidationException() {
        Role role = new Role("BANK_MANAGER", "Bank manager");
        AdminUser user = new AdminUser("bank.manager", "bank.manager@hooperbank.local", "password123", role);
        Session session = new Session();
        session.setToken("expired-token");
        session.setAdminUser(user);
        session.setActive(true);
        session.setExpiresAt(Instant.now().minusSeconds(60));

        when(sessionRepository.findByToken("expired-token")).thenReturn(Optional.of(session));

        assertThrows(ValidationException.class, () -> authService.validateSession("expired-token"));
    }
}
