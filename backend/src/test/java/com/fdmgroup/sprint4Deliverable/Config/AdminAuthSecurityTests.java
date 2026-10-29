package com.fdmgroup.sprint4Deliverable.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.Role;
import com.fdmgroup.sprint4Deliverable.Model.Session;
import com.fdmgroup.sprint4Deliverable.Repository.AdminUserRepository;
import com.fdmgroup.sprint4Deliverable.Repository.RoleRepository;
import com.fdmgroup.sprint4Deliverable.Repository.SessionRepository;
import com.fdmgroup.sprint4Deliverable.Security.TokenDigest;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAuthSecurityTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AdminUserRepository adminUserRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private SessionRepository sessionRepository;

    private String username;
    private String password;

    @BeforeEach
    void createAdminUser() {
        username = "security-test-" + UUID.randomUUID();
        password = "legacy-password";
        Role role = roleRepository.findByName("BANK_TELLER")
                .orElseGet(() -> roleRepository.save(new Role("BANK_TELLER", "Bank teller")));
        adminUserRepository.save(new AdminUser(username, username + "@example.test", password, role));
    }

    @Test
    void successfulLoginUpgradesLegacyPasswordAndOnlyPersistsTokenDigest() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("BANK_TELLER"))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andReturn();

        String token = result.getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        AdminUser savedUser = adminUserRepository.findByUsername(username).orElseThrow();
        Session savedSession = sessionRepository.findByAdminUserAndActiveTrue(savedUser).orElseThrow();

        assertTrue(savedUser.getPassword().startsWith("{bcrypt}"));
        assertNotEquals(password, savedUser.getPassword());
        assertEquals(TokenDigest.sha256(token), savedSession.getTokenDigest());

        mockMvc.perform(get("/api/auth/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void unknownAndIncorrectCredentialsHaveTheSamePublicFailure() throws Exception {
        String knownFailure = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"wrong"}
                                """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("authentication_failed"))
                .andReturn().getResponse().getContentAsString();

        String unknownFailure = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"unknown-%s","password":"wrong"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("authentication_failed"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(knownFailure, unknownFailure);
    }

    @Test
    void protectedOperationsRequireAdminTokenAndAcceptValidTellerSession() throws Exception {
        mockMvc.perform(get("/customer/getAll"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("authentication_required"));
        mockMvc.perform(get("/account/getAll"))
                .andExpect(status().isUnauthorized());

        String token = loginAndGetToken();
        mockMvc.perform(get("/customer/getAll").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void managerCanSignInWithTheSameCurrentAdminAccess() throws Exception {
        String managerUsername = "manager-" + UUID.randomUUID();
        Role managerRole = roleRepository.findByName("BANK_MANAGER")
                .orElseGet(() -> roleRepository.save(new Role("BANK_MANAGER", "Bank manager")));
        adminUserRepository.save(new AdminUser(managerUsername, managerUsername + "@example.test",
                "manager-password", managerRole));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"manager-password"}
                                """.formatted(managerUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("BANK_MANAGER"));
    }

    @Test
    void logoutRevokesTokenAndIsSafeWhenRepeated() throws Exception {
        String token = loginAndGetToken();
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("Signed out successfully"));

        mockMvc.perform(get("/customer/getAll").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void malformedAndExpiredTokensCannotAccessProtectedOperations() throws Exception {
        mockMvc.perform(get("/customer/getAll").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());

        String token = loginAndGetToken();
        AdminUser adminUser = adminUserRepository.findByUsername(username).orElseThrow();
        Session session = sessionRepository.findByAdminUserAndActiveTrue(adminUser).orElseThrow();
        session.setExpiresAt(java.time.Instant.now().minusSeconds(1));
        sessionRepository.save(session);

        mockMvc.perform(get("/customer/getAll").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledAdminAndNonAdminRoleCannotAccessProtectedOperations() throws Exception {
        String token = loginAndGetToken();
        AdminUser adminUser = adminUserRepository.findByUsername(username).orElseThrow();
        adminUser.setEnabled(false);
        adminUserRepository.save(adminUser);

        mockMvc.perform(get("/customer/getAll").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        String nonAdminUsername = "non-admin-" + UUID.randomUUID();
        Role nonAdminRole = roleRepository.findByName("CUSTOMER_TEST")
                .orElseGet(() -> roleRepository.save(new Role("CUSTOMER_TEST", "Not an administrator")));
        adminUserRepository.save(new AdminUser(nonAdminUsername, nonAdminUsername + "@example.test",
                "non-admin-password", nonAdminRole));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"non-admin-password"}
                                """.formatted(nonAdminUsername)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("authentication_failed"));
    }

    @Test
    void activeSessionWithChangedNonAdminRoleIsForbidden() throws Exception {
        String token = loginAndGetToken();
        AdminUser adminUser = adminUserRepository.findByUsername(username).orElseThrow();
        Role nonAdminRole = roleRepository.findByName("CUSTOMER_TEST")
                .orElseGet(() -> roleRepository.save(new Role("CUSTOMER_TEST", "Not an administrator")));
        adminUser.setRole(nonAdminRole);
        adminUserRepository.save(adminUser);

        mockMvc.perform(get("/customer/getAll").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("access_denied"));
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }
}
