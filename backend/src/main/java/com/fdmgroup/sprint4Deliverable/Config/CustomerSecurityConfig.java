package com.fdmgroup.sprint4Deliverable.Config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fdmgroup.sprint4Deliverable.Model.CustomerAccount;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerAccountRepository;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class CustomerSecurityConfig {

	private final ObjectMapper objectMapper;

	public CustomerSecurityConfig(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Bean
	PasswordEncoder customerPasswordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	UserDetailsService customerUserDetailsService(CustomerAccountRepository repository) {
		return username -> {
			CustomerAccount account = repository.findByUsername(username)
					.orElseThrow(() -> new UsernameNotFoundException("Customer account not found"));
			if (!"ACTIVE".equals(account.getStatus())) {
				throw new UsernameNotFoundException("Customer account not found");
			}
			return User.withUsername(account.getUsername())
					.password(account.getPasswordHash())
					.authorities("ROLE_CUSTOMER")
					.build();
		};
	}

	@Bean
	DaoAuthenticationProvider customerAuthenticationProvider(
			UserDetailsService customerUserDetailsService, PasswordEncoder customerPasswordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customerUserDetailsService);
		provider.setPasswordEncoder(customerPasswordEncoder);
		return provider;
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	SecurityContextRepository customerSecurityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	CorsConfigurationSource customerCorsConfigurationSource(
			@Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
				.map(String::trim).filter(origin -> !origin.isEmpty()).toList());
		configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/customer-auth/**", configuration);
		return source;
	}

	@Bean
	@Order(1)
	SecurityFilterChain customerSecurityFilterChain(
			HttpSecurity http,
			CorsConfigurationSource customerCorsConfigurationSource,
			SecurityContextRepository customerSecurityContextRepository,
			DaoAuthenticationProvider customerAuthenticationProvider) throws Exception {
		http.securityMatcher("/api/customer-auth/**")
				.cors(cors -> cors.configurationSource(customerCorsConfigurationSource))
				.csrf(csrf -> {
					HttpSessionCsrfTokenRepository tokenRepository = new HttpSessionCsrfTokenRepository();
					tokenRepository.setHeaderName("X-CSRF-TOKEN");
					csrf.csrfTokenRepository(tokenRepository);
				})
				.securityContext(context -> context.securityContextRepository(customerSecurityContextRepository))
				.authenticationProvider(customerAuthenticationProvider)
				.sessionManagement(session -> session
						.sessionFixation(fixation -> fixation.changeSessionId())
						.maximumSessions(-1))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/api/customer-auth/csrf").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/customer-auth/register",
								"/api/customer-auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/customer-auth/session").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/customer-auth/logout").hasRole("CUSTOMER")
						.anyRequest().hasRole("CUSTOMER"))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) ->
								writeSecurityError(response, HttpStatus.UNAUTHORIZED,
										"session_expired", "Your session has expired. Please sign in again."))
						.accessDeniedHandler((request, response, exception) ->
								writeSecurityError(response, HttpStatus.FORBIDDEN,
										"access_denied", "You do not have access to this resource.")))
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable());
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain compatibilitySecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
				.csrf(csrf -> csrf.disable())
				.formLogin(form -> form.disable())
				.httpBasic(basic -> basic.disable());
		return http.build();
	}

	private void writeSecurityError(HttpServletResponse response, HttpStatus status,
			String code, String message) throws java.io.IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), java.util.Map.of("code", code, "message", message));
	}
}
