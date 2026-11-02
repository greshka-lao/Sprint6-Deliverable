package com.fdmgroup.sprint4Deliverable.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fdmgroup.sprint4Deliverable.Model.Company;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationRequest;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationRequest.AddressRequest;
import com.fdmgroup.sprint4Deliverable.Model.Person;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;
import com.fdmgroup.sprint4Deliverable.Service.CustomerAuthService.CustomerRateLimitException;
import com.fdmgroup.sprint4Deliverable.Service.CustomerAuthService.RegistrationValidationException;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTests {

	@Mock
	private CustomerRepository customerRepository;
	
	@Mock
	private AddressRepository addressRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	private CustomerUsernameService usernameService;
	private CustomerAuthService customerAuthService;

	@BeforeEach
	void setUp() {
		usernameService = new CustomerUsernameService(customerRepository);
		customerAuthService = new CustomerAuthService(
				customerRepository, addressRepository, usernameService, passwordEncoder, authenticationManager);
	}

	@Test
	void registerCreatesCustomerOnlyIdentityAndReturnsGeneratedUsername() {
		when(customerRepository.existsByUsername("greshkalao001")).thenReturn(false);
		when(passwordEncoder.encode("customer-password")).thenReturn("{bcrypt}encoded");
		when(customerRepository.saveAndFlush(any(Customer.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		var response = customerAuthService.register(validRequest());

		assertEquals("greshkalao001", response.username());
		assertEquals("Greshka Lao", response.name());
		verify(customerRepository).saveAndFlush(any(Customer.class));
		ArgumentCaptor<Customer> savedCustomer = ArgumentCaptor.forClass(Customer.class);
		verify(customerRepository).saveAndFlush(savedCustomer.capture());
		assertEquals("person", savedCustomer.getValue().getCustomerType());
		assertTrue(savedCustomer.getValue() instanceof Person);
		assertEquals(0L, savedCustomer.getValue().getCustomerId());
	}

	@Test
	void registerCreatesTheSelectedCompanyProfile() {
		when(customerRepository.existsByUsername("greshkalao001")).thenReturn(false);
		when(passwordEncoder.encode("customer-password")).thenReturn("{bcrypt}encoded");
		when(customerRepository.saveAndFlush(any(Customer.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		customerAuthService.register(validRequest("company"));

		ArgumentCaptor<Customer> savedCustomer = ArgumentCaptor.forClass(Customer.class);
		verify(customerRepository).saveAndFlush(savedCustomer.capture());
		assertEquals("company", savedCustomer.getValue().getCustomerType());
		assertTrue(savedCustomer.getValue() instanceof Company);
		assertEquals(0L, savedCustomer.getValue().getCustomerId());
	}

	@Test
	void registrationRejectsMissingCustomerType() {
		CustomerRegistrationRequest request = new CustomerRegistrationRequest(
				"Greshka Lao",
				new AddressRequest("1 Main Street", "A1A 1A1", "Example City", "Example Province"),
				"greshka@example.com", "+15551234567", "customer-password", null);

		assertThrows(RegistrationValidationException.class,
				() -> customerAuthService.register(request));
	}

	@Test
	void registrationRejectsMalformedPhoneNumber() {
		CustomerRegistrationRequest request = new CustomerRegistrationRequest(
				"Greshka Lao",
				new AddressRequest("1 Main Street", "A1A 1A1", "Example City", "Example Province"),
				"greshka@example.com", "555-1234", "customer-password", "person");

		assertThrows(RegistrationValidationException.class,
				() -> customerAuthService.register(request));
	}

	@Test
	void concurrentUsernameCollisionRetriesNextSuffix() {
		when(customerRepository.existsByUsername("greshkalao001")).thenReturn(false, true);
		when(passwordEncoder.encode("customer-password")).thenReturn("{bcrypt}encoded");
		when(customerRepository.saveAndFlush(any(Customer.class)))
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
		return validRequest("person");
	}

	private CustomerRegistrationRequest validRequest(String customerType) {
		return new CustomerRegistrationRequest(
				"Greshka Lao",
				new AddressRequest("1 Main Street", "A1A 1A1", "Example City", "Example Province"),
				"Greshka@example.com", "+15551234567", "customer-password", customerType);
	}
}
