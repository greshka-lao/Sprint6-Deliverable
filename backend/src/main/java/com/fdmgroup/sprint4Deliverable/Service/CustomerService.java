package com.fdmgroup.sprint4Deliverable.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityNotFoundException;

import com.fdmgroup.sprint4Deliverable.Exception.IncorrectCityOrProvinceException;
import com.fdmgroup.sprint4Deliverable.Model.Account;
import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.AdminCustomerListItem;
import com.fdmgroup.sprint4Deliverable.Model.Company;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Model.Person;
import com.fdmgroup.sprint4Deliverable.Repository.AccountRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;
import com.fdmgroup.sprint4Deliverable.Util.Location;
import com.fdmgroup.sprint4Deliverable.Util.PostalCodeLookup;

@Service
public class CustomerService {
	
	private CustomerRepository customerRepo;
	private AddressRepository addressRepo;
	private AccountRepository accountRepo; 
	
	@Autowired
	public CustomerService(CustomerRepository customerRepo, AddressRepository addressRepo, AccountRepository accountRepo) {
		super();
		this.customerRepo = customerRepo;
		this.addressRepo = addressRepo;
		this.accountRepo = accountRepo;
	}
	
	public void saveCustomer(Customer customer) throws Exception {
		if (customer == null || customer.getCustomerType() == null || customer.getAddress() == null
				|| customer.getName() == null || customer.getName().isBlank()) {
			throw new IllegalArgumentException("Customer type is required.");
		}
		if (customer.getCustomerId() != 0) {
			throw new IllegalArgumentException("Customer identifiers are server-managed.");
		}
		customer.setCustomerType(customer.getCustomerType());
		if (("person".equals(customer.getCustomerType()) && !(customer instanceof Person))
				|| ("company".equals(customer.getCustomerType()) && !(customer instanceof Company))) {
			throw new IllegalArgumentException("Customer type must match its profile subtype.");
		}
		customer.setUsername(null);
		customer.setEmail(null);
		customer.setPhoneNumber(null);
		customer.setPasswordHash(null);
		customer.setRole("CUSTOMER");
		customer.setStatus("ACTIVE");
		customerRepo.save(customer);
	}

	public Customer findCustomerById(long id) {
		return customerRepo.findById(id).orElse(null);
	}
	
	public List<Account> getAccounts(long id) {
		return customerRepo.findById(id).map(Customer::getAccounts).orElse(List.of());
	}
	
	/*
	 * ASSUMPTION: Street number is also edited for this method.
	 */
	public void editCustomer(Customer customer) throws Exception {
		if (customer == null || customer.getAddress() == null) {
			throw new IllegalArgumentException("Customer address is required.");
		}
		Customer current = customerRepo.findById(customer.getCustomerId())
				.orElseThrow(() -> new EntityNotFoundException("Customer not found."));
		Address currentAddress = current.getAddress();
		Address submittedAddress = customer.getAddress();
		if (currentAddress == null) {
			throw new IllegalStateException("Customer address is missing.");
		}
		Location location = PostalCodeLookup.findLocation(submittedAddress.getPostalCode());
		if ((!isBlank(submittedAddress.getCity()) && !location.city().equals(submittedAddress.getCity()))
				|| (!isBlank(submittedAddress.getProvince())
						&& !location.province().equals(submittedAddress.getProvince()))) {
			throw new IncorrectCityOrProvinceException();
		}
		current.setName(customer.getName());
		currentAddress.setStreetNumber(submittedAddress.getStreetNumber());
		currentAddress.setPostalCode(submittedAddress.getPostalCode());
		currentAddress.setCity(location.city());
		currentAddress.setProvince(location.province());
		addressRepo.save(currentAddress);
		customerRepo.save(current);
	}
	
	public void addAccount(long customerId, long accountId) {
		Customer customer = customerRepo.findById(customerId)
				.orElseThrow(() -> new IllegalArgumentException("Customer not found."));
		Account account = accountRepo.findById(accountId)
				.orElseThrow(() -> new IllegalArgumentException("Account not found."));
		account.setCustomer(customer);
		accountRepo.save(account);
	}
	
	public boolean deleteCustomer(long id) {
		Customer customer = customerRepo.findById(id).orElse(null);
		if (customer == null) {
			return false;
		}
		for (Account account : customer.getAccounts()) {
			account.setCustomer(null);
		}
		accountRepo.saveAll(customer.getAccounts());
		customer.setUsername(null);
		customer.setEmail(null);
		customer.setPhoneNumber(null);
		customer.setPasswordHash(null);
		customer.setStatus("DELETED");
		customerRepo.save(customer);
		return true;
	}
	
	public List<AdminCustomerListItem> getAllCustomers() {
		return customerRepo.findByStatus("ACTIVE").stream().map(customer -> {
			Address address = customer.getAddress();
			String addressText = address == null ? "" : java.util.stream.Stream.of(
					address.getStreetNumber(), address.getCity(), address.getProvince(), address.getPostalCode())
					.filter(value -> value != null && !value.isBlank())
					.collect(java.util.stream.Collectors.joining(", "));
			return new AdminCustomerListItem(customer.getCustomerId(), customer.getName(),
					customer.getUsername(), addressText, customer.getCustomerType(),
					customer.getPhoneNumber(), customer.getEmail());
		}).toList();
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
