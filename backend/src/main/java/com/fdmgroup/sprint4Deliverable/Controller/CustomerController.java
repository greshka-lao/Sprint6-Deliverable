package com.fdmgroup.sprint4Deliverable.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fdmgroup.sprint4Deliverable.Model.Account;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Service.AddressService;
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

	@Autowired
	public CustomerController(CustomerService customerService, AddressService addressService) {
		super();
		this.customerService = customerService;
		this.addressService = addressService;
	}
	
	@Operation(summary = "Create a new customer")
	@PostMapping("/save")
	public ResponseEntity<?> saveCustomer(@RequestBody Customer customer) {
		
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
	public Customer getCustomerById(@Parameter(description = "The ID of the specified customer") @RequestParam long customerId) {
		return customerService.findCustomerById(customerId);
	}
	
	@Operation(summary = "Edit customer details: name, street number, postal codel, city, province")
	@PutMapping("/edit")
	public ResponseEntity<?> editCustomer(@RequestBody Customer customer) {
		
		try {
			customerService.editCustomer(customer);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
		
		return ResponseEntity.ok().build();
	}
	
	@Operation(summary = "Get all accounts connected to a customer")
	@GetMapping("/getAccounts")
	public List<Account> getAccounts(@Parameter(description = "The ID of the specified customer") @RequestParam long customerId) {
		return customerService.getAccounts(customerId);
	}
	
	@Operation(summary = "Delete a customer")
	@DeleteMapping("/delete")
	public void deleteCustomer(@Parameter(description = "The ID of the specified customer") @RequestParam long customerId) {
		customerService.deleteCustomer(customerId);
	}
	
	@Operation(summary = "Get all customers")
	@GetMapping("/getAll")
	public List<Customer> getAllCustomers() {
		return customerService.getAllCustomers();
	}
}
