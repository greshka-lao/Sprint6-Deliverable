package com.fdmgroup.sprint4Deliverable.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fdmgroup.sprint4Deliverable.Model.Account;
import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.CheckingAccount;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Model.CustomerDashboardResponse;
import com.fdmgroup.sprint4Deliverable.Model.CustomerDashboardResponse.AccountTotal;
import com.fdmgroup.sprint4Deliverable.Model.CustomerDashboardResponse.CustomerAccount;
import com.fdmgroup.sprint4Deliverable.Model.CustomerDashboardResponse.CustomerProfile;
import com.fdmgroup.sprint4Deliverable.Model.CustomerDashboardResponse.PostalAddress;
import com.fdmgroup.sprint4Deliverable.Model.SavingsAccount;
import com.fdmgroup.sprint4Deliverable.Repository.AccountRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;

@Service
public class CustomerDashboardService {
	private static final String CURRENCY = "NZD";
	private static final int FRACTION_DIGITS = 2;

	private final CustomerRepository customerRepository;
	private final AccountRepository accountRepository;

	public CustomerDashboardService(
			CustomerRepository customerRepository,
			AccountRepository accountRepository) {
		this.customerRepository = customerRepository;
		this.accountRepository = accountRepository;
	}

	@Transactional(readOnly = true)
	public CustomerDashboardResponse getDashboard(String username) {
		Customer customer = customerRepository.findByUsername(username)
				.filter(account -> "ACTIVE".equals(account.getStatus()))
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.UNAUTHORIZED, "Customer session is no longer valid."));

		Address address = customer.getAddress();
		CustomerProfile profile = new CustomerProfile(
				customer.getName(),
				customer.getEmail(),
				customer.getPhoneNumber(),
				address == null ? null : new PostalAddress(
						address.getStreetNumber(),
						address.getCity(),
						address.getProvince(),
						address.getPostalCode()));

		List<Account> customerAccounts = accountRepository.findByCustomer(customer);
		List<CustomerAccount> accounts = customerAccounts.stream()
				.map(this::toCustomerAccount)
				.toList();

		BigDecimal total = customerAccounts.stream()
				.map(account -> BigDecimal.valueOf(account.getBalance()))
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(FRACTION_DIGITS, RoundingMode.HALF_UP);

		return new CustomerDashboardResponse(
				profile,
				accounts,
				new AccountTotal(total, CURRENCY));
	}

	private CustomerAccount toCustomerAccount(Account account) {
		String accountType;
		if (account instanceof SavingsAccount) {
			accountType = "savingsAccount";
		} else if (account instanceof CheckingAccount) {
			accountType = "checkingAccount";
		} else {
			throw new IllegalStateException("Unsupported customer account type.");
		}

		BigDecimal balance = BigDecimal.valueOf(account.getBalance())
				.setScale(FRACTION_DIGITS, RoundingMode.HALF_UP);
		return new CustomerAccount(account.getAccountId(), accountType, balance, CURRENCY);
	}
}
