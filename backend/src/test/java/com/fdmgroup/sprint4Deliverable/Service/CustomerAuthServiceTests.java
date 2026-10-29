package com.fdmgroup.sprint4Deliverable.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fdmgroup.sprint4Deliverable.Model.CustomerAccount;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationRequest;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationRequest.AddressRequest;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerAccountRepository;
import com.fdmgroup.sprint4Deliverable.Service.CustomerAuthService.CustomerRateLimitException;
import com.fdmgroup.sprint4Deliverable.Service.CustomerAuthService.RegistrationValidationException;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTests {

	@Mock
	private CustomerAccountRepository repository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	private CustomerUsernameService usernameService;
	private CustomerAuthService customerAuthService;

	@BeforeEach
	void setUp() {
		usernameService = new CustomerUsernameService(repository);
		customerAuthService = new CustomerAuthService(
				repository, usernameService, passwordEncoder, authenticationManager);
	}

	@Test
	void registerCreatesCustomerOnlyIdentityAndReturnsGeneratedUsername() {
		when(repository.existsByUsername("greshkalao001")).thenReturn(false);
		when(passwordEncoder.encode("customer-password")).thenReturn("{bcrypt}encoded");
		when(repository.saveAndFlush(any(CustomerAccount.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = customerAuthService.register(validRequest());

		assertEquals("greshkalao001", response.username());
		assertEquals("Greshka Lao", response.name());
		verify(repository).saveAndFlush(any(CustomerAccount.class));
	}

	@Test
	void registrationRejectsMalformedPhoneNumber() {
		CustomerRegistrationRequest request = new CustomerRegistrationRequest(
				"Greshka Lao",
				new AddressRequest("1 Main Street", "A1A 1A1", "Example City", "Example Province"),
				"greshka@example.com", "555-1234", "customer-password");

		assertThrows(RegistrationValidationException.class,
				() -> customerAuthService.register(request));
	}

	@Test
	void concurrentUsernameCollisionRetriesNextSuffix() {
		when(repository.existsByUsername("greshkalao001")).thenReturn(false, true);
		when(passwordEncoder.encode("customer-password")).thenReturn("{bcrypt}encoded");
		when(repository.saveAndFlush(any(CustomerAccount.class)))
				.thenThrow(new DataIntegrityViolationException("username collision"))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = customerAuthService.register(validRequest());

		assertEquals("greshkalao002", response.username());
	}

	@Test
	void repeatedFailedLoginIsTemporarilyRateLimited() {
		when(authenticationManager.authenticate(any()))
				.thenThrow(new BadCredentialsException("Invalid username or password."));
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getRemoteAddr()).thenReturn("192.0.2.1");

		for (int attempt = 0; attempt < 5; attempt++) {
			assertThrows(BadCredentialsException.class,
					() -> customerAuthService.authenticate("greshkalao001", "wrong-password", request));
		}

		CustomerRateLimitException exception = assertThrows(CustomerRateLimitException.class,
				() -> customerAuthService.authenticate("greshkalao001", "wrong-password", request));
		assertEquals("Sign-in is temporarily unavailable. Please try again later.", exception.getMessage());
	}

	private CustomerRegistrationRequest validRequest() {
		return new CustomerRegistrationRequest(
				"Greshka Lao",
				new AddressRequest("1 Main Street", "A1A 1A1", "Example City", "Example Province"),
				"Greshka@example.com", "+15551234567", "customer-password");
	}
}
