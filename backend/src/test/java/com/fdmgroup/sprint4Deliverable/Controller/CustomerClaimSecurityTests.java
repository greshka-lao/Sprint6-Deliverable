package com.fdmgroup.sprint4Deliverable.Controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fdmgroup.sprint4Deliverable.Model.Company;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;
import com.fdmgroup.sprint4Deliverable.Service.CustomerAuthService;
import com.fdmgroup.sprint4Deliverable.Service.CustomerUsernameService;

@ExtendWith(MockitoExtension.class)
class CustomerClaimSecurityTests {

	@Mock
	private CustomerRepository customerRepository;
	
	@Mock
	private AddressRepository addressRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Test
	void unclaimedProfileCannotBeUsedAsACustomerLogin() {
		Customer profile = new Company("FDM Group", null, null);
		profile.setUsername("fdmgroup001");
		profile.setEmail("claim@example.com");
		profile.setPasswordHash(null);
		when(customerRepository.findByUsername("fdmgroup001")).thenReturn(Optional.of(profile));

		CustomerAuthService authService = new CustomerAuthService(customerRepository, addressRepository,
				new CustomerUsernameService(customerRepository), passwordEncoder, authenticationManager);

		assertThrows(BadCredentialsException.class,
				() -> authService.customerForUsername("fdmgroup001"));
	}
}
