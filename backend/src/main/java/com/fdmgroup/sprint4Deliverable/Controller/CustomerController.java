package com.fdmgroup.sprint4Deliverable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fdmgroup.sprint4Deliverable.Model.Account;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Service.AddressService;
import com.fdmgroup.sprint4Deliverable.Service.AuthService;
import com.fdmgroup.sprint4Deliverable.Service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
    name = "Customer",
    description = "Operations for managing customers"
)
@RestController
@RequestMapping("/customer")
public class CustomerController {

	private CustomerService customerService;
	private AddressService addressService;
	private AuthService authService;

	@Autowired
	public CustomerController(CustomerService customerService, AddressService addressService, AuthService authService) {
		super();
		this.customerService = customerService;
		this.addressService = addressService;
		this.authService = authService;
	}
	
	@Operation(summary = "Create a new customer")
	@PostMapping("/save")
	public ResponseEntity<?> saveCustomer(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, @RequestBody Customer customer) {
		authService.validateSession(extractToken(authorizationHeader));
		try {
			addressService.saveAddress(customer.getAddress());
			customerService.saveCustomer(customer);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
		
		return ResponseEntity.ok().build();
	}
	
	@Operation(summary = "Get a customer by ID")
	@GetMapping("/get")
	public Customer getCustomerById(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, @Parameter(description = "The ID of the specified customer") @RequestParam long customerId) {
		authService.validateSession(extractToken(authorizationHeader));
		return customerService.findCustomerById(customerId);
	}
	
	@Operation(summary = "Edit customer details: name, street number, postal codel, city, province")
	@PutMapping("/edit")
	public ResponseEntity<?> editCustomer(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, @RequestBody Customer customer) {
		authService.validateSession(extractToken(authorizationHeader));
		try {
			customerService.editCustomer(customer);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
		
		return ResponseEntity.ok().build();
	}
	
	@Operation(summary = "Get all accounts connected to a customer")
	@GetMapping("/getAccounts")
	public List<Account> getAccounts(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, @Parameter(description = "The ID of the specified customer") @RequestParam long customerId) {
		authService.validateSession(extractToken(authorizationHeader));
		return customerService.getAccounts(customerId);
	}
	
	@Operation(summary = "Delete a customer")
	@DeleteMapping("/delete")
	public void deleteCustomer(@RequestHeader(value = "Authorization", required = false) String authorizationHeader, @Parameter(description = "The ID of the specified customer") @RequestParam long customerId) {
		authService.validateSession(extractToken(authorizationHeader));
		customerService.deleteCustomer(customerId);
	}
	
	@Operation(summary = "Get all customers")
	@GetMapping("/getAll")
	public List<Customer> getAllCustomers(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		authService.validateSession(extractToken(authorizationHeader));
		return customerService.getAllCustomers();
	}

	private String extractToken(String authorizationHeader) {
		if (authorizationHeader == null || authorizationHeader.isBlank()) {
			return null;
		}
		String[] parts = authorizationHeader.split(" ");
		if (parts.length == 2 && "Bearer".equalsIgnoreCase(parts[0])) {
			return parts[1];
		}
		return authorizationHeader;
	}
}
