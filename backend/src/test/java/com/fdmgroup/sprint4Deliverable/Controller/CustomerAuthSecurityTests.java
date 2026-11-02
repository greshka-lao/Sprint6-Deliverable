package com.fdmgroup.sprint4Deliverable.Controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fdmgroup.sprint4Deliverable.Model.CheckingAccount;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.Person;
import com.fdmgroup.sprint4Deliverable.Model.SavingsAccount;
import com.fdmgroup.sprint4Deliverable.Repository.AccountRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerAuthSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private AddressRepository addressRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Test
	void csrfEndpointReturnsSessionBoundToken() throws Exception {
		mockMvc.perform(get("/api/customer-auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void customerSessionEndpointReturnsDocumentedAnonymousResponse() throws Exception {
		mockMvc.perform(get("/api/customer-auth/session"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.authenticated").value(false))
				.andExpect(jsonPath("$.code").value("session_expired"));
	}

	@Test
	void dashboardRejectsAnonymousRequests() throws Exception {
		mockMvc.perform(get("/api/customer-auth/dashboard"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registrationRejectsRequestWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/customer-auth/register")
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void registrationValidatesBodyAfterCsrfCheck() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get("/api/customer-auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		String token = csrfResult.getResponse().getContentAsString()
				.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
		MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);

		mockMvc.perform(post("/api/customer-auth/register")
						.session(session)
						.header("X-CSRF-TOKEN", token)
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("validation_error"));
	}

	@Test
	@Transactional
	void registeredCustomerCanSignInValidateSessionAndLogout() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get("/api/customer-auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		String token = tokenFrom(csrfResult);
		MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
		String email = UUID.randomUUID() + "@example.com";
		String name = "Test Customer " + UUID.randomUUID();
		String requestBody = """
				{
				  "name": "%s",
				  "address": {
				    "streetNumber": "1 Main Street",
				    "postalCode": "A1A 1A1",
				    "city": "Example City",
				    "province": "Example Province"
				  },
				  "email": "%s",
				  "phoneNumber": "+15551234567",
				  "password": "customer-password",
				  "customerType": "person"
				}
				""".formatted(name, email);

		MvcResult registration = mockMvc.perform(post("/api/customer-auth/register")
						.session(session)
						.header("X-CSRF-TOKEN", token)
						.contentType("application/json")
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").exists())
				.andReturn();
		String username = registration.getResponse().getContentAsString()
				.replaceAll(".*\"username\":\"([^\"]+)\".*", "$1");
		session = (MockHttpSession) registration.getRequest().getSession(false);
		Customer customer = customerRepository.findByUsername(username).orElseThrow();
		accountRepository.saveAndFlush(new SavingsAccount(1234.56, customer, 0.02));
		accountRepository.saveAndFlush(new CheckingAccount(987.65, customer, 123));

		Address otherAddress = new Address();
		otherAddress.setStreetNumber("99 Other Road");
		otherAddress.setPostalCode("B2B 2B2");
		otherAddress.setCity("Other City");
		otherAddress.setProvince("Other Province");
		addressRepository.saveAndFlush(otherAddress);
		String otherIdentity = UUID.randomUUID().toString().replace("-", "");
		Person otherCustomer = new Person("Other Customer", otherAddress, null);
		otherCustomer.setUsername("other" + otherIdentity);
		otherCustomer.setEmail(otherIdentity + "@example.com");
		otherCustomer.setPhoneNumber("+15551234568");
		otherCustomer.setPasswordHash("test-password-hash");
		customerRepository.saveAndFlush(otherCustomer);
		accountRepository.saveAndFlush(new CheckingAccount(5000, otherCustomer, 456));

		MvcResult freshCsrf = mockMvc.perform(get("/api/customer-auth/csrf").session(session))
				.andExpect(status().isOk())
				.andReturn();
		token = tokenFrom(freshCsrf);
		MvcResult login = mockMvc.perform(post("/api/customer-auth/login")
						.session(session)
						.header("X-CSRF-TOKEN", token)
						.contentType("application/json")
						.content("""
								{"username":"%s","password":"customer-password"}
								""".formatted(username)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("CUSTOMER"))
				.andReturn();
		session = (MockHttpSession) login.getRequest().getSession(false);

		mockMvc.perform(get("/api/customer-auth/session").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true))
				.andExpect(jsonPath("$.customer.username").value(username));

		mockMvc.perform(get("/api/customer-auth/dashboard").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.profile.name").value(name))
				.andExpect(jsonPath("$.profile.email").value(email))
				.andExpect(jsonPath("$.profile.phoneNumber").value("+15551234567"))
				.andExpect(jsonPath("$.profile.address.streetNumber").value("1 Main Street"))
				.andExpect(jsonPath("$.accounts.length()").value(2))
				.andExpect(jsonPath("$.accounts[0].currency").value("NZD"))
				.andExpect(jsonPath("$.accounts[1].currency").value("NZD"))
				.andExpect(jsonPath("$.total.amount").value(2222.21))
				.andExpect(jsonPath("$.total.currency").value("NZD"));

		mockMvc.perform(get("/customer/getAll").session(session))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("authentication_required"));

		MvcResult logoutCsrf = mockMvc.perform(get("/api/customer-auth/csrf").session(session))
				.andExpect(status().isOk())
				.andReturn();
		mockMvc.perform(post("/api/customer-auth/logout")
						.session(session)
						.header("X-CSRF-TOKEN", tokenFrom(logoutCsrf)))
				.andExpect(status().isNoContent());
	}

	private String tokenFrom(MvcResult result) throws Exception {
		return result.getResponse().getContentAsString()
				.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
	}
}
