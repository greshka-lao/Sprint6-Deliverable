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

@SpringBootTest
@AutoConfigureMockMvc
class CustomerAuthSecurityTests {

	@Autowired
	private MockMvc mockMvc;

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
				  "password": "customer-password"
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
