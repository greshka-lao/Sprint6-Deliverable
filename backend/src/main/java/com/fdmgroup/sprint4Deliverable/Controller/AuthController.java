package com.fdmgroup.sprint4Deliverable.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.AuthResponse;
import com.fdmgroup.sprint4Deliverable.Model.LoginRequest;
import com.fdmgroup.sprint4Deliverable.Service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(
                request == null ? null : request.getUsername(),
                request == null ? null : request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        authService.logout(token);
        return ResponseEntity.ok(new AuthResponse(null, null, null, "Signed out successfully"));
    }

    @GetMapping("/session")
    public ResponseEntity<AuthResponse> session(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        String token = extractToken(authorizationHeader);
        AdminUser adminUser = authService.validateSession(token);
        return ResponseEntity.ok(new AuthResponse(null, adminUser.getUsername(),
                adminUser.getRole().getName(), "Session valid"));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String[] parts = authorizationHeader.split(" ");
        if (parts.length == 2 && "Bearer".equalsIgnoreCase(parts[0])) {
            return parts[1];
        }
        return authorizationHeader;
    }
}
