package com.fdmgroup.sprint4Deliverable.Controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fdmgroup.sprint4Deliverable.Exception.AdminAuthenticationException;
import com.fdmgroup.sprint4Deliverable.Service.AddressService;
import com.fdmgroup.sprint4Deliverable.Service.AuthService;
import com.fdmgroup.sprint4Deliverable.Service.CustomerService;

@ExtendWith(MockitoExtension.class)
class CustomerManagementSecurityTests {

	@Mock
	private AuthService authService;

	@Mock
	private CustomerService customerService;

	@Mock
	private AddressService addressService;

	@Test
	void customerListRequiresAnAdministratorSession() {
		when(authService.validateSession(null)).thenThrow(new AdminAuthenticationException(
				HttpStatus.UNAUTHORIZED, "unauthorized", "Authentication is required."));
		CustomerController controller = new CustomerController(customerService, addressService, authService);

		assertThrows(AdminAuthenticationException.class, () -> controller.getAllCustomers(null));
	}
}
