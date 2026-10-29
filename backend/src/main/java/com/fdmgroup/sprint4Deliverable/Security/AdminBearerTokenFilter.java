package com.fdmgroup.sprint4Deliverable.Security;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Service.AuthService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminBearerTokenFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBearerTokenFilter.class);

    private final AuthService authService;

    public AdminBearerTokenFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request.getHeader("Authorization"));
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<AdminUser> adminUser;
            try {
                adminUser = authService.authenticateToken(token);
            } catch (DataAccessException exception) {
                LOGGER.error("Administrator session lookup failed ({})", exception.getClass().getSimpleName());
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"service_unavailable\",\"message\":\"The service is temporarily unavailable.\"}");
                return;
            }
            adminUser.ifPresent(user -> {
                AdminUserDetails principal = new AdminUserDetails(user);
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String[] parts = authorizationHeader.trim().split("\\s+", 2);
        if (parts.length == 2 && "Bearer".equalsIgnoreCase(parts[0])) {
            return parts[1].isBlank() ? null : parts[1];
        }
        return authorizationHeader.trim();
    }
}
