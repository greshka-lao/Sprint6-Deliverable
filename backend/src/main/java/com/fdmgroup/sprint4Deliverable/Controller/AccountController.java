package com.fdmgroup.sprint4Deliverable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fdmgroup.sprint4Deliverable.Model.Account;
import com.fdmgroup.sprint4Deliverable.Service.AccountService;
import com.fdmgroup.sprint4Deliverable.Service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
    name = "Account",
    description = "Operations for managing accounts"
)
@RestController
@RequestMapping("/account")
public class AccountController {

	private AccountService accountService;
	private CustomerService customerService;

	@Autowired
	public AccountController(AccountService accountService, CustomerService customerService) {
		super();
		this.accountService = accountService;
		this.customerService = customerService;
	}
	
	@Operation(summary = "Create a new account")
	@PostMapping("/save")
	public Account saveCustomer(@RequestBody Account account) {
		return accountService.saveAccount(account);
	}
	
	@Operation(summary = "Edit account details")
	@PutMapping("/edit")
	public Account editAccount(@RequestBody Account account) {
		return accountService.saveAccount(account);
	}
	
	@Operation(summary = "Delete account")
	@DeleteMapping("/delete")
	public void deleteAccount(@Parameter(description = "The ID of the specified account") @RequestParam long accountId) {
		accountService.deleteAccount(accountId);
	}
	
	@Operation(summary = "Get an account by ID")
	@GetMapping("/get")
	public Account getCustomerById(@Parameter(description = "The ID of the specified account") @RequestParam long accountId) {
		return accountService.findAccountById(accountId);
	}
	
	@Operation(summary = "Get all accounts with customers in a specified city")
	@GetMapping("/getByCity")
	public List<Account> getByCity(@Parameter(description = "The specified city") @RequestParam String city) {
		return accountService.findByCity(city);
	}
	
	@Operation(summary = "Assign a customer to account")
	@PutMapping("/setCustomer")
	public void setCustomer(@Parameter(description = "The ID of the specified account") @RequestParam long accountId, @Parameter(description = "The ID of the specified customer") @RequestParam long customerId) {
		accountService.setCustomer(accountId, customerId);
		customerService.addAccount(customerId, accountId);
	}
	
	@Operation(summary = "Get all accounts")
	@GetMapping("/getAll")
	public List<Account> getAllAccounts() {
		return accountService.getAllAccounts();
	}
}
