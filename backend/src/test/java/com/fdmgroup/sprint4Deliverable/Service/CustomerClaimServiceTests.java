package com.fdmgroup.sprint4Deliverable.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.Company;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Model.CustomerClaimRequest;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationRequest.AddressRequest;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerClaimRequestRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerClaimServiceTests {

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private CustomerClaimRequestRepository claimRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	private CustomerClaimService claimService;

	@BeforeEach
	void setUp() {
		claimService = new CustomerClaimService(customerRepository, claimRepository,
				new CustomerUsernameService(customerRepository), passwordEncoder);
	}

	@Test
	void claimStoresOnlyHashAndDoesNotAttachCredentials() {
		Customer customer = companyCustomer();
		when(customerRepository.findByNameAndCustomerType("FDM Group", "company"))
				.thenReturn(List.of(customer));
		when(customerRepository.existsByEmail("claim@example.com")).thenReturn(false);
		when(claimRepository.existsByCustomer_CustomerIdAndState(12L, "PENDING")).thenReturn(false);

		var response = claimService.submit("FDM Group", "company", addressRequest(),
				"Claim@example.com", "+15551234567");

		ArgumentCaptor<CustomerClaimRequest> requestCaptor = ArgumentCaptor.forClass(CustomerClaimRequest.class);
		verify(claimRepository).save(requestCaptor.capture());
		CustomerClaimRequest savedRequest = requestCaptor.getValue();
		assertEquals("PENDING", savedRequest.getState());
		assertEquals(64, savedRequest.getClaimReferenceHash().length());
		assertFalse(response.claimReference().equals(savedRequest.getClaimReferenceHash()));
		assertEquals(null, customer.getUsername());
		assertEquals(null, customer.getEmail());
		assertEquals(null, customer.getPasswordHash());
	}

	@Test
	void ambiguousProfilesDoNotCreateClaimOrRevealProfileExistence() {
		when(customerRepository.findByNameAndCustomerType("FDM Group", "company"))
				.thenReturn(List.of(companyCustomer(), companyCustomer()));

		var response = claimService.submit("FDM Group", "company", addressRequest(),
				"claim@example.com", "+15551234567");

		assertEquals("UNAVAILABLE", response.state());
		assertEquals(null, response.claimReference());
		assertEquals(0, claimRepository.count());
	}

	@Test
	void onlyPendingClaimsCanBeReviewedAndOnlyApprovedClaimsCompleted() {
		Customer customer = companyCustomer();
		CustomerClaimRequest request = new CustomerClaimRequest(
				customer, "a".repeat(64), "claim@example.com", "+15551234567");
		when(claimRepository.findById(17L)).thenReturn(Optional.of(request));
		claimService.approve(17L, "admin");
		assertEquals("APPROVED", request.getState());
		assertEquals(null, customer.getUsername());
		assertEquals(null, customer.getPasswordHash());

		when(claimRepository.findByClaimReferenceHash(anyString())).thenReturn(Optional.of(request));
		when(customerRepository.existsByEmail("claim@example.com")).thenReturn(false);
		when(passwordEncoder.encode("new-password")).thenReturn("{bcrypt}encoded");
		when(customerRepository.saveAndFlush(any(Customer.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		var completed = claimService.complete("single-use-reference", "new-password");

		assertEquals("fdmgroup001", completed.username());
		assertEquals("COMPLETED", request.getState());
		assertEquals("{bcrypt}encoded", customer.getPasswordHash());
		assertEquals("claim@example.com", customer.getEmail());
		verify(claimRepository, times(2)).save(request);
		assertThrows(CustomerClaimService.InvalidClaimReferenceException.class,
				() -> claimService.status("single-use-reference"));
	}

	@Test
	void rejectedClaimCannotBeCompleted() {
		CustomerClaimRequest request = new CustomerClaimRequest(
				companyCustomer(), "b".repeat(64), "claim@example.com", "+15551234567");
		request.reject("admin");
		when(claimRepository.findByClaimReferenceHash(anyString())).thenReturn(Optional.of(request));

		assertThrows(CustomerClaimService.InvalidClaimReferenceException.class,
				() -> claimService.complete("rejected-reference", "new-password"));
	}

	@Test
	void invalidClaimReferenceCannotRevealAStatus() {
		when(claimRepository.findByClaimReferenceHash(anyString())).thenReturn(Optional.empty());

		assertThrows(CustomerClaimService.InvalidClaimReferenceException.class,
				() -> claimService.status("invalid-reference"));
	}

	private static Customer companyCustomer() {
		Address address = new Address();
		address.setStreetNumber("1 Main Street");
		address.setPostalCode("A1A 1A1");
		address.setCity("Example City");
		address.setProvince("Example Province");
		Customer customer = new Company("FDM Group", address, null);
		customer.setCustomerId(12L);
		return customer;
	}

	private static AddressRequest addressRequest() {
		return new AddressRequest("1 Main Street", "A1A 1A1", "Example City", "Example Province");
	}
}
