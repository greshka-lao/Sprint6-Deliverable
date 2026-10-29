package com.fdmgroup.sprint4Deliverable.Controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fdmgroup.sprint4Deliverable.Model.CustomerAuthResponse;
import com.fdmgroup.sprint4Deliverable.Model.CustomerCsrfTokenResponse;
import com.fdmgroup.sprint4Deliverable.Model.CustomerLoginRequest;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationRequest;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationResponse;
import com.fdmgroup.sprint4Deliverable.Model.CustomerSessionResponse;
import com.fdmgroup.sprint4Deliverable.Service.CustomerAuthService;
import com.fdmgroup.sprint4Deliverable.Service.CustomerAuthService.CustomerRateLimitException;
import com.fdmgroup.sprint4Deliverable.Service.CustomerAuthService.DuplicateCustomerEmailException;
import com.fdmgroup.sprint4Deliverable.Service.CustomerAuthService.RegistrationValidationException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/customer-auth")
public class CustomerAuthController {

	private final CustomerAuthService customerAuthService;
	private final SecurityContextRepository securityContextRepository;

	public CustomerAuthController(CustomerAuthService customerAuthService,
			SecurityContextRepository customerSecurityContextRepository) {
		this.customerAuthService = customerAuthService;
		this.securityContextRepository = customerSecurityContextRepository;
	}

	@GetMapping("/csrf")
	public CustomerCsrfTokenResponse csrf(HttpServletRequest request) {
		Object tokenAttribute = request.getAttribute(CsrfToken.class.getName());
		if (!(tokenAttribute instanceof CsrfToken token)) {
			throw new IllegalStateException("CSRF token was not initialized for the customer request.");
		}
		return new CustomerCsrfTokenResponse(token.getToken());
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody(required = false) CustomerRegistrationRequest request) {
		try {
			CustomerRegistrationResponse response = customerAuthService.register(request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (RegistrationValidationException exception) {
			return ResponseEntity.badRequest().body(Map.of(
					"code", "validation_error",
					"message", "Enter all required details and correct any invalid values."));
		} catch (DuplicateCustomerEmailException exception) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
					"code", "duplicate_identity",
					"message", "That email address is already in use."));
		}
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody(required = false) CustomerLoginRequest request,
			HttpServletRequest servletRequest, HttpServletResponse response) {
		if (request == null || request.username() == null || request.password() == null) {
			return authenticationFailure();
		}

		try {
			CustomerAuthResponse customer = customerAuthService.authenticate(
					request.username(), request.password(), servletRequest);
			if (servletRequest.getSession(false) != null) {
				servletRequest.changeSessionId();
			}
			var session = servletRequest.getSession(true);
			session.setMaxInactiveInterval(15 * 60);
			session.setAttribute("CUSTOMER_AUTH_SESSION", Boolean.TRUE);

			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			securityContextRepository.saveContext(context, servletRequest, response);
			return ResponseEntity.ok(customer);
		} catch (CustomerRateLimitException exception) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
					.header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
					.body(Map.of("code", "rate_limited",
							"message", "Sign-in is temporarily unavailable. Please try again later."));
		} catch (AuthenticationException exception) {
			return authenticationFailure();
		}
	}

	@GetMapping("/session")
	public ResponseEntity<Map<String, Object>> session(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()
				|| !authentication.getAuthorities().stream()
						.anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()))) {
			CustomerSessionResponse expired = CustomerSessionResponse.expired();
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					"authenticated", false,
					"code", expired.code(),
					"message", expired.message()));
		}
		try {
			return ResponseEntity.ok(Map.of(
					"authenticated", true,
					"customer", customerAuthService.customerForUsername(authentication.getName())));
		} catch (BadCredentialsException exception) {
			CustomerSessionResponse expired = CustomerSessionResponse.expired();
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					"authenticated", false,
					"code", expired.code(),
					"message", expired.message()));
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		SecurityContextHolder.clearContext();
		if (request.getSession(false) != null) {
			request.getSession(false).invalidate();
		}
		return ResponseEntity.noContent().build();
	}

	private ResponseEntity<Map<String, String>> authenticationFailure() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
				"code", "authentication_failed",
				"message", "Invalid username or password."));
	}
}
