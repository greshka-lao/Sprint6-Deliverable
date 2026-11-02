package com.fdmgroup.sprint4Deliverable.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fdmgroup.sprint4Deliverable.Model.Account;
import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.AdminCustomerListItem;
import com.fdmgroup.sprint4Deliverable.Model.Company;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Model.Person;
import com.fdmgroup.sprint4Deliverable.Repository.AccountRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerManagementTests {

	@Mock
	private CustomerRepository customerRepository;

	@Mock
	private AddressRepository addressRepository;

	@Mock
	private AccountRepository accountRepository;

	private CustomerService customerService;

	@BeforeEach
	void setUp() {
		customerService = new CustomerService(customerRepository, addressRepository, accountRepository);
	}

	@Test
	void administratorCreationPersistsTheSelectedProfileTypeWithoutCredentials() throws Exception {
		Customer customer = new Person("Greshka Lao", address(), null);

		customerService.saveCustomer(customer);

		assertEquals("person", customer.getCustomerType());
		assertInstanceOf(Person.class, customer);
		assertEquals(null, customer.getUsername());
		assertEquals(null, customer.getPasswordHash());
		verify(customerRepository).save(customer);
	}

	@Test
	void editingCannotChangeCustomerTypeOrProfileAssociation() throws Exception {
		Customer stored = new Person("Greshka Lao", address(), null);
		stored.setCustomerId(9L);
		when(customerRepository.findById(9L)).thenReturn(Optional.of(stored));
		Customer submitted = new Company("Updated Name",
				new Address("15 Queen Street", "M5H 3P5", "Toronto", "Ontario"), null);
		submitted.setCustomerId(9L);

		customerService.editCustomer(submitted);

		assertEquals("person", stored.getCustomerType());
		assertInstanceOf(Person.class, stored);
		assertEquals("Updated Name", stored.getName());
		verify(customerRepository).save(stored);
	}

	@Test
	void deletionRemovesLoginAndAssignmentsWhilePreservingJoinedProfile() {
		Customer customer = new Person("Greshka Lao", address(), null);
		customer.setCustomerId(9L);
		customer.setUsername("greshkalao001");
		customer.setEmail("greshka@example.com");
		customer.setPhoneNumber("+14165550100");
		customer.setPasswordHash("encoded");
		Account account = new Account() {
		};
		account.setCustomer(customer);
		customer.setAccounts(new ArrayList<>(java.util.List.of(account)));
		when(customerRepository.findById(9L)).thenReturn(Optional.of(customer));

		customerService.deleteCustomer(9L);

		assertEquals(null, account.getCustomer());
		assertEquals(null, customer.getUsername());
		assertEquals(null, customer.getEmail());
		assertEquals(null, customer.getPhoneNumber());
		assertEquals(null, customer.getPasswordHash());
		assertEquals("DELETED", customer.getStatus());
		assertInstanceOf(Person.class, customer);
		verify(accountRepository).saveAll(customer.getAccounts());
		verify(customerRepository).save(customer);
	}

	@Test
	void activeCustomerListMapsRequestedFieldsAndConcatenatesAddress() {
		Customer customer = new Person("Greshka Lao", address(), null);
		customer.setCustomerId(9L);
		customer.setUsername("greshkalao001");
		customer.setPhoneNumber("+14165550100");
		customer.setEmail("greshka@example.com");
		when(customerRepository.findByStatus("ACTIVE")).thenReturn(List.of(customer));

		List<AdminCustomerListItem> result = customerService.getAllCustomers();

		assertEquals(1, result.size());
		AdminCustomerListItem item = result.get(0);
		assertEquals(9L, item.customerId());
		assertEquals("Greshka Lao", item.name());
		assertEquals("greshkalao001", item.username());
		assertEquals("12 Queen Street, Toronto, Ontario, M5H 3P5", item.address());
		assertEquals("person", item.customerType());
		assertEquals("+14165550100", item.phoneNumber());
		assertEquals("greshka@example.com", item.email());
		verify(customerRepository).findByStatus("ACTIVE");
	}

	@Test
	void activeCustomerListAllowsMissingValues() {
		Customer customer = new Company("FDM Group", new Address(), null);
		when(customerRepository.findByStatus("ACTIVE")).thenReturn(List.of(customer));

		AdminCustomerListItem item = customerService.getAllCustomers().get(0);

		assertEquals("", item.address());
		assertEquals(null, item.username());
		assertEquals(null, item.phoneNumber());
		assertEquals(null, item.email());
	}

	private static Address address() {
		Address address = new Address();
		address.setStreetNumber("12 Queen Street");
		address.setPostalCode("M5H 3P5");
		address.setCity("Toronto");
		address.setProvince("Ontario");
		return address;
	}
}
