package com.fdmgroup.sprint4Deliverable.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fdmgroup.sprint4Deliverable.Exception.IncorrectCityOrProvinceException;
import com.fdmgroup.sprint4Deliverable.Model.Account;
import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Repository.AccountRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;
import com.fdmgroup.sprint4Deliverable.Util.Location;
import com.fdmgroup.sprint4Deliverable.Util.PostalCodeLookup;

@Service
public class CustomerService {
	
	private CustomerRepository customerRepo;
//	private AddressRepository addressRepo;
	private AccountRepository accountRepo; 
	
	@Autowired
	public CustomerService(CustomerRepository customerRepo, AddressRepository addressRepo, AccountRepository accountRepo) {
		super();
		this.customerRepo = customerRepo;
//		this.addressRepo = addressRepo;
		this.accountRepo = accountRepo;
	}
	
	public void saveCustomer(Customer customer) throws Exception {
		customerRepo.save(customer);
	}
	
	public Customer findCustomerById(long id) {
		Optional<Customer> foundCustomer = customerRepo.findById(id);
		return foundCustomer.isPresent() ? foundCustomer.get() : null;
	}
	
	public List<Account> getAccounts(long id) {
		Optional<Customer> foundCustomer = customerRepo.findById(id);
		return foundCustomer.isPresent() ? foundCustomer.get().getAccounts() : null;
	}
	
	/*
	 * ASSUMPTION: Street number is also edited for this method.
	 */
	public void editCustomer(Customer customer) throws Exception {
		Optional<Customer> foundCustomer = customerRepo.findById(customer.getCustomerId());
		if (foundCustomer.isPresent()) {
			
			// Set new name
			foundCustomer.get().setName(customer.getName());
			
			// Obtain current and new addresses
			Address customerAddress = foundCustomer.get().getAddress();
			Address newAddress = customer.getAddress();
			
			// Set new street name, postal code, city, province
			customerAddress.setStreetNumber(newAddress.getStreetNumber());
			
			// Find postal code
			Location location = PostalCodeLookup.findLocation(newAddress.getPostalCode());
			
			customerAddress.setPostalCode(newAddress.getPostalCode());
			
			if ((!newAddress.getCity().isBlank() && !location.city().equals(newAddress.getCity())) 
			 || (!newAddress.getProvince().isBlank() && !location.province().equals(newAddress.getProvince()))) {
				throw new IncorrectCityOrProvinceException();
			}
			
			customerAddress.setCity(location.city());
			customerAddress.setProvince(location.province());

			// Set current address to new address
			foundCustomer.get().setAddress(customerAddress);
			
			customerRepo.save(foundCustomer.get());
		}
	}
	
	
	public void addAccount(long customerId, long accountId) {
		Optional<Customer> customer = customerRepo.findById(customerId);
		Optional<Account> account = accountRepo.findById(accountId);
		
		if (customer.isPresent() && account.isPresent()) {
			
			Customer foundCustomer = customer.get();
			Account foundAccount = account.get();
			
			List<Account> accounts = foundCustomer.getAccounts();
			if (accounts != null) {
				accounts.add(foundAccount);
			} else {
				accounts = new ArrayList<>();
			}
			foundCustomer.setAccounts(accounts);
			customerRepo.save(foundCustomer);
		}
	}
	
	public void deleteCustomer(long id) {
		Optional<Customer> foundCustomer = customerRepo.findById(id);
		if (foundCustomer.isPresent()) {
			customerRepo.delete(foundCustomer.get());
		}
	}
	
	public List<Customer> getAllCustomers() {
		return customerRepo.findAll();
	}
}
