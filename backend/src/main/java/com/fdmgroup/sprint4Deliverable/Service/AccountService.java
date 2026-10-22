package com.fdmgroup.sprint4Deliverable.Service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fdmgroup.sprint4Deliverable.Model.Account;
import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Repository.AccountRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;

@Service
public class AccountService {
	
	private AccountRepository accountRepo;
	private CustomerRepository customerRepo;
	private AddressRepository addressRepo;
	
	@Autowired
	public AccountService(AccountRepository accountRepo, CustomerRepository customerRepo, AddressRepository addressRepo) {
		super();
		this.accountRepo = accountRepo;
		this.customerRepo = customerRepo;
		this.addressRepo = addressRepo;
	}
	
	public Account saveAccount(Account account) {
		
		Customer customer = account.getCustomer();
		
		if (customer != null) {
			Address address = customer.getAddress();

			addressRepo.save(address);
			customerRepo.save(customer);
		}

		return accountRepo.save(account);
	}
	
	public void deleteAccount(long id) {
		Optional<Account> foundAccount = accountRepo.findById(id);
		if (foundAccount.isPresent()) {
			accountRepo.delete(foundAccount.get());
		}
	}
	
	public Account findAccountById(long id) {
		Optional<Account> foundAccount = accountRepo.findById(id);
		return foundAccount.isPresent() ? foundAccount.get() : null;
	}
	
	public void setCustomer(long accountId, long customerId) {
		Optional<Account> account = accountRepo.findById(accountId);
		Optional<Customer> customer = customerRepo.findById(customerId);
		
		if (account.isPresent() && customer.isPresent()) {
			
			Account foundAccount = account.get();
			Customer foundCustomer = customer.get();
			
			foundAccount.setCustomer(foundCustomer);
			foundAccount.setCustomerId(foundCustomer.getCustomerId());
			
			accountRepo.save(foundAccount);
		}
	}
	
	public List<Account> findByCity(String city) {
		
		// Get full list of accounts
		List<Account> fullList = accountRepo.findAll();
		
		// Filter by if their customer is based in the specified city
		return fullList.stream()
					   .filter(a -> city.equals(a.getCustomer() != null ? a.getCustomer().getAddress().getCity()
							   										  : null))
					   .toList();
	}
	
	public List<Account> getAllAccounts() {
		return accountRepo.findAll();
	}
}
