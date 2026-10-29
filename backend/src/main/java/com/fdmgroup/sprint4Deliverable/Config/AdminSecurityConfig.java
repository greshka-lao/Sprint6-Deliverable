package com.fdmgroup.sprint4Deliverable.Config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fdmgroup.sprint4Deliverable.Security.AdminBearerTokenFilter;
import com.fdmgroup.sprint4Deliverable.Security.AdminUserDetails;
import com.fdmgroup.sprint4Deliverable.Security.AdminUserDetailsService;
import com.fdmgroup.sprint4Deliverable.Security.LegacyAwarePasswordEncoder;
import com.fdmgroup.sprint4Deliverable.Service.AdminSecurityEventService;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class AdminSecurityConfig {

    private final ObjectMapper objectMapper;

    public AdminSecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    DaoAuthenticationProvider adminAuthenticationProvider(
            AdminUserDetailsService adminUserDetailsService,
            LegacyAwarePasswordEncoder adminPasswordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(adminUserDetailsService);
        provider.setPasswordEncoder(adminPasswordEncoder);
        provider.setUserDetailsPasswordService(adminUserDetailsService);
        return provider;
    }

    @Bean(name = "adminAuthenticationManager")
    AuthenticationManager adminAuthenticationManager(DaoAuthenticationProvider adminAuthenticationProvider) {
        return new ProviderManager(adminAuthenticationProvider);
    }

    @Bean
    CorsConfigurationSource adminCorsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/auth/**", configuration);
        source.registerCorsConfiguration("/customer/**", configuration);
        source.registerCorsConfiguration("/account/**", configuration);
        return source;
    }

    @Bean
    FilterRegistrationBean<AdminBearerTokenFilter> disableContainerRegistration(
            AdminBearerTokenFilter adminBearerTokenFilter) {
        FilterRegistrationBean<AdminBearerTokenFilter> registration =
                new FilterRegistrationBean<>(adminBearerTokenFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(2)
    SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource adminCorsConfigurationSource,
            DaoAuthenticationProvider adminAuthenticationProvider,
            AdminBearerTokenFilter adminBearerTokenFilter,
            @Qualifier("adminAuthenticationManager") AuthenticationManager adminAuthenticationManager,
            AdminSecurityEventService securityEventService) throws Exception {
        http.securityMatcher("/api/auth/**", "/customer/**", "/account/**")
                .cors(cors -> cors.configurationSource(adminCorsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .securityContext(context -> context
                        .securityContextRepository(new NullSecurityContextRepository()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authenticationManager(adminAuthenticationManager)
                .authenticationProvider(adminAuthenticationProvider)
                .addFilterBefore(adminBearerTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/session").permitAll()
                        .requestMatchers("/customer/**", "/account/**").hasAnyRole("BANK_TELLER", "BANK_MANAGER")
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            securityEventService.record(null, "ACCESS_DENIED", "UNAUTHENTICATED",
                                    auditSubject(request.getRequestURI()));
                            writeSecurityError(response, HttpStatus.UNAUTHORIZED,
                                    "authentication_required", "Please sign in to continue.");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            securityEventService.record(null, "ACCESS_DENIED", "FORBIDDEN",
                                    auditSubject(request.getRequestURI()));
                            writeSecurityError(response, HttpStatus.FORBIDDEN,
                                    "access_denied", "You do not have access to this resource.");
                        }))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    private void writeSecurityError(
            jakarta.servlet.http.HttpServletResponse response,
            HttpStatus status,
            String code,
            String message) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of("error", code, "message", message));
    }

    private String auditSubject(String requestPath) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminUserDetails adminUser) {
            return adminUser.getUsername();
        }
        return requestPath;
    }
}
