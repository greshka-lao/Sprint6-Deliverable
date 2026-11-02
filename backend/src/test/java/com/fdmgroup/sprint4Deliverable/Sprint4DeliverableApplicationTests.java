package com.fdmgroup.sprint4Deliverable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import jakarta.servlet.Filter;
import jakarta.persistence.EntityNotFoundException;

import com.fdmgroup.sprint4Deliverable.Exception.IncorrectCityOrProvinceException;
import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.Account;
import com.fdmgroup.sprint4Deliverable.Model.AdminCustomerListItem;
import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.CheckingAccount;
import com.fdmgroup.sprint4Deliverable.Model.Company;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Model.Person;
import com.fdmgroup.sprint4Deliverable.Model.SavingsAccount;
import com.fdmgroup.sprint4Deliverable.Model.Role;
import com.fdmgroup.sprint4Deliverable.Controller.AccountController;
import com.fdmgroup.sprint4Deliverable.Controller.CustomerController;
import com.fdmgroup.sprint4Deliverable.Repository.AccountRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Repository.AdminUserRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;
import com.fdmgroup.sprint4Deliverable.Repository.RoleRepository;
import com.fdmgroup.sprint4Deliverable.Service.AccountService;
import com.fdmgroup.sprint4Deliverable.Service.AddressService;
import com.fdmgroup.sprint4Deliverable.Service.AuthService;
import com.fdmgroup.sprint4Deliverable.Service.CustomerService;
import com.fdmgroup.sprint4Deliverable.Util.Location;
import com.fdmgroup.sprint4Deliverable.Util.PostalCodeLookup;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class Sprint4DeliverableApplicationTests {

	private static Customer person(String name, Address address) {
		return new Person(name, address, null);
	}

	private static Customer company(String name, Address address) {
		return new Company(name, address, null);
	}

	private MockMvc customerControllerMvc(CustomerService service) {
		return MockMvcBuilders.standaloneSetup(new CustomerController(
				service, mock(AddressService.class), mock(AuthService.class))).build();
	}

	private MockMvc accountControllerMvc(AccountService service) {
		return MockMvcBuilders.standaloneSetup(new AccountController(
				service, mock(CustomerService.class), mock(AuthService.class))).build();
	}
	
	ObjectMapper objectMapper = new ObjectMapper();
	
	@Autowired
	private MockMvc mvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	@Qualifier("springSecurityFilterChain")
	private Filter springSecurityFilterChain;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private RoleRepository roleRepository;

	private String adminUsername;
	private String adminPassword = "test-admin-password";
	
	@Mock
    private CustomerRepository customerRepo;
	
	@Mock
    private AddressRepository addressRepo;
	
	@Mock
	private AccountRepository accountRepo;

    @InjectMocks
    private CustomerService customerService;
    
    @InjectMocks
    private AccountService accountService;
    
    @InjectMocks
    private AddressService addressService;

    @BeforeEach
    void authenticateAdminRequests() throws Exception {
        adminUsername = "legacy-api-test-" + UUID.randomUUID();
        Role role = roleRepository.findByName("BANK_TELLER")
                .orElseGet(() -> roleRepository.save(new Role("BANK_TELLER", "Bank teller")));
        adminUserRepository.save(new AdminUser(adminUsername, adminUsername + "@example.test", adminPassword, role));

        MvcResult login = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(adminUsername, adminPassword)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        String token = login.getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .defaultRequest(MockMvcRequestBuilders.get("/")
                        .header("Authorization", "Bearer " + token))
                .build();
    }

    @Test
    public void contextLoads() {
    }
	
    @Test
    public void applicationClassIsBootstrapped() {
        assertNotNull(Sprint4DeliverableApplication.class
                .getAnnotation(SpringBootApplication.class));
    }
	
	@Test
	public void savePerson_returns200() throws Exception {
		
		Address newAddress = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
		Customer newCustomer = person("John Doe", newAddress);
		
		String customerJson = objectMapper.writeValueAsString(newCustomer);
		
		mvc.perform(MockMvcRequestBuilders.post("/customer/save")
										  .contentType(MediaType.APPLICATION_JSON)
										  .content(customerJson))
		   .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void saveCompany_returns200() throws Exception {
		
		Address newAddress = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
		Customer newCustomer = company("FDM Group", newAddress);
		
		String customerJson = objectMapper.writeValueAsString(newCustomer);
		
		mvc.perform(MockMvcRequestBuilders.post("/customer/save")
										  .contentType(MediaType.APPLICATION_JSON)
										  .content(customerJson))
		   .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void findCustomerById_returns200() throws Exception {
		Long id = 1L;

		Customer expectedCustomer = company("Test", new Address());
		expectedCustomer.setCustomerId(id);
		when(customerRepo.findById(id)).thenReturn(Optional.of(expectedCustomer));

		mvc = customerControllerMvc(customerService);
		mvc.perform(MockMvcRequestBuilders.get("/customer/get")
	            						  .param("customerId", Long.toString(id)))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void findCustomerById_ifPresent_returnsCustomer() {
		
		Long id = 1L;

        Customer expectedCustomer = company("Test", new Address());
        expectedCustomer.setCustomerId(id);

        when(customerRepo.findById(id))
            .thenReturn(Optional.of(expectedCustomer));

        Customer actualCustomer = customerService.findCustomerById(id);

        assertEquals(expectedCustomer, actualCustomer);
    } 
	
	@Test
	public void findCustomerById_ifNotPresent_returnsNull() {
		
		Long id = 1L;

        when(customerRepo.findById(id))
            .thenReturn(Optional.empty());

        Customer actualCustomer = customerService.findCustomerById(id);

        assertEquals(null, actualCustomer);
    } 

	@Test
	public void editPerson_returns200() throws Exception {
		// New customer data coming from the request
		Long id = 1L;
	    Address newAddress = new Address("697 Collins Street", "3000", "Melbourne", "Victoria");
	    Customer newCustomer = person("Greshka Lao", newAddress);
	    newCustomer.setCustomerId(id);

	    String customerJson = objectMapper.writeValueAsString(newCustomer);

		mvc = customerControllerMvc(mock(CustomerService.class));
	    mvc.perform(MockMvcRequestBuilders.put("/customer/edit")
	            						  .contentType(MediaType.APPLICATION_JSON)
	            						  .content(customerJson))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void editCompany_returns200() throws Exception {
		// New customer data coming from the request
		Long id = 1L;
	    Address newAddress = new Address("14 Show Place", "8011", "Christchurch", "Canterbury");
	    Customer newCustomer = company("IAG", newAddress);
	    newCustomer.setCustomerId(id);

	    String customerJson = objectMapper.writeValueAsString(newCustomer);

		mvc = customerControllerMvc(mock(CustomerService.class));
	    mvc.perform(MockMvcRequestBuilders.put("/customer/edit")
	            						  .contentType(MediaType.APPLICATION_JSON)
	            						  .content(customerJson))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}

	@Test
	public void editPerson_ifPresent_callsRepo_updatesCustomer() throws Exception {
		
		Long id = 1L;

	    // Existing customer in the database
	    Address existingAddress = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
	    Customer existingCustomer = person("John Doe", existingAddress);
	    existingCustomer.setCustomerId(id);

	    // New customer data coming from the request
	    Address newAddress = new Address("697 Collins Street", "3000", "Melbourne", "Victoria");
	    Customer newCustomer = person("Greshka Lao", newAddress);
	    newCustomer.setCustomerId(id);
	    
	    when(customerRepo.findById(id)).thenReturn(Optional.of(existingCustomer));

	    customerService.editCustomer(newCustomer);

	    // Check that the existing customer was updated
	    assertEquals("Greshka Lao", existingCustomer.getName());
	    assertEquals("697 Collins Street", existingCustomer.getAddress().getStreetNumber());
	    assertEquals("3000", existingCustomer.getAddress().getPostalCode());
	    assertEquals("Melbourne", existingCustomer.getAddress().getCity());
	    assertEquals("Victoria", existingCustomer.getAddress().getProvince());
	    
	    verify(customerRepo).save(existingCustomer);
	}
	
	@Test
	public void editPerson_ifNotPresent_noChange() throws Exception {
		
		Long id = 1L;

	    // Existing customer in the database
	    Address existingAddress = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
	    Customer existingCustomer = person("John Doe", existingAddress);
	    existingCustomer.setCustomerId(id);

	    // New customer data coming from the request
	    Address newAddress = new Address("697 Collins Street", "3000", "Melbourne", "Victoria");
	    Customer newCustomer = person("Greshka Lao", newAddress);
	    newCustomer.setCustomerId(id);
	    
	    when(customerRepo.findById(id)).thenReturn(Optional.empty());

	    assertThrows(EntityNotFoundException.class, () -> customerService.editCustomer(newCustomer));

	    assertEquals("John Doe", existingCustomer.getName());
	    assertEquals("300 Barangaroo Avenue", existingCustomer.getAddress().getStreetNumber());
	    assertEquals("2000", existingCustomer.getAddress().getPostalCode());
	    assertEquals("Sydney", existingCustomer.getAddress().getCity());
	    assertEquals("New South Wales", existingCustomer.getAddress().getProvince());
	    
	    // Ensure that the repository was never updated
	    verify(customerRepo, never()).save(existingCustomer);
	}
	
	@Test
	public void editCompany_ifPresent_callsRepo_updatesCustomer() throws Exception {
		
		Long id = 1L; 

	    // Existing customer in the database
	    Address existingAddress = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
	    Customer existingCustomer = company("FDM Group", existingAddress);
	    existingCustomer.setCustomerId(id);

	    // New customer data coming from the request
	    Address newAddress = new Address("14 Show Place", "8011", "Christchurch", "Canterbury");
	    Customer newCustomer = company("IAG", newAddress);
	    newCustomer.setCustomerId(id);
	    
	    when(customerRepo.findById(id)).thenReturn(Optional.of(existingCustomer));

	    customerService.editCustomer(newCustomer);

	    // Check that the existing customer was updated
	    assertEquals("IAG", existingCustomer.getName());
	    assertEquals("14 Show Place", existingCustomer.getAddress().getStreetNumber());
	    assertEquals("8011", existingCustomer.getAddress().getPostalCode());
	    assertEquals("Christchurch", existingCustomer.getAddress().getCity());
	    assertEquals("Canterbury", existingCustomer.getAddress().getProvince());
	    
	    verify(customerRepo).save(existingCustomer);
	}
	
	@Test
	public void deleteCustomer_returnsNoContent() throws Exception {
		Long id = 60L;
		CustomerService service = mock(CustomerService.class);
		when(service.deleteCustomer(id)).thenReturn(true);

		mvc = customerControllerMvc(service);
		mvc.perform(MockMvcRequestBuilders.delete("/customer/delete")
	            						  .param("customerId", Long.toString(id)))
	       .andExpect(MockMvcResultMatchers.status().isNoContent());
	}
	
	@Test
	public void deleteCustomer_callsRepo_softDeletesCustomer() {

		Long id = 1L;
		
	    Customer customer = company("Test", new Address());
	    customer.setCustomerId(id);

	    when(customerRepo.findById(id)).thenReturn(Optional.of(customer));

	    assertTrue(customerService.deleteCustomer(id));

	    verify(customerRepo).findById(id);
	    assertEquals("DELETED", customer.getStatus());
	    verify(customerRepo).save(customer);
	    verify(customerRepo, never()).delete(customer);
	}
	
	@Test
	public void getAllCustomers_returns200() throws Exception {
		
		mvc.perform(MockMvcRequestBuilders.get("/customer/getAll"))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void getAllCustomers_callsRepo_returnsList() throws Exception {

		// Create addresses
		Address address1 = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
		Address address2 = new Address("697 Collins Street", "3000", "Melbourne", "Victoria");
		
		// Expected customers
		List<Customer> expected = new ArrayList<>();
		expected.add(person("John Doe", address1));
		expected.add(company("FDM Group", address2));
	    
	    expected.get(0).setCustomerId(1L);
	    expected.get(1).setCustomerId(2L);
	    
	    when(customerRepo.findByStatus("ACTIVE")).thenReturn(expected);

	    List<AdminCustomerListItem> actual = customerService.getAllCustomers();
	    
	    verify(customerRepo).findByStatus("ACTIVE");
	    assertEquals(List.of(
            new AdminCustomerListItem(1, "John Doe", null,
                "300 Barangaroo Avenue, Sydney, New South Wales, 2000", "person", null, null),
            new AdminCustomerListItem(2, "FDM Group", null,
                "697 Collins Street, Melbourne, Victoria, 3000", "company", null, null)), actual);
	}
	
	@Test
	public void findCityAndProvince_1010_ReturnAucklandAuckland() throws Exception {
		
		Location expected = new Location("Auckland", "Auckland");
		Location actual = PostalCodeLookup.findLocation("1010");
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void findCityAndProvince_1234_ThrowException() {
		
		try {
			PostalCodeLookup.findLocation("1234");
			fail();
		} catch (Exception e) {
		}
		
	}
	
	@Test
	public void createAucklandAddressWithStreetNumberAndPostalCode_AddsCityAndProvince() throws Exception {
		
		Address address = new Address("152 Fanshawe Street", "1010");
		
		assertEquals("Auckland", address.getCity());
		assertEquals("Auckland", address.getProvince());
	}
	
	@Test
	public void createDunedinAddressWithStreetNumberAndPostalCode_AddsCityAndProvince() throws Exception {
		
		Address address = new Address("1 Baldwin Street", "9010");
		
		assertEquals("Dunedin", address.getCity());
		assertEquals("Otago", address.getProvince());
	}
	
	@Test
	public void saveCustomer_OnlyStreetNumberAndPostcode_returns200() throws Exception {
		
		Address newAddress = new Address("Hungerford Crescent", "3216");
		Customer newCustomer = person("John Doe", newAddress);
		
		String customerJson = objectMapper.writeValueAsString(newCustomer);
		
		mvc.perform(MockMvcRequestBuilders.post("/customer/save")
										  .contentType(MediaType.APPLICATION_JSON)
										  .content(customerJson))
		   .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void postalCodeMap_Find_BrisbaneQueensland() throws Exception {
		
		String postalCode = "4000";
		Location expectedLocation = new Location("Brisbane", "Queensland");
		
		Location actualLocation = PostalCodeLookup.findLocation(postalCode);
		
		assertEquals(expectedLocation, actualLocation);
	}
	
	@Test
	public void saveSavingsAccount_returns200() throws Exception {
		
		Address newAddress = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
		Customer newCustomer = person("John Doe", newAddress);
		SavingsAccount newAccount = new SavingsAccount(1234.56, newCustomer, 0.05);
		
		String accountJson = objectMapper.writeValueAsString(newAccount);
		
		mvc.perform(MockMvcRequestBuilders.post("/account/save")
										  .contentType(MediaType.APPLICATION_JSON)
										  .content(accountJson))
		   .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void saveCheckingAccount_returns200() throws Exception {
		
		Address newAddress = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
		Customer newCustomer = person("John Doe", newAddress);
		CheckingAccount newAccount = new CheckingAccount(6789.01, newCustomer, 10);
		
		String accountJson = objectMapper.writeValueAsString(newAccount);
		
		mvc.perform(MockMvcRequestBuilders.post("/account/save")
										  .contentType(MediaType.APPLICATION_JSON)
										  .content(accountJson))
		   .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void account_setBalance() {
		
		Account account = new SavingsAccount(1234.56, person("Test", new Address()), 0.05);
		double newBalance = 6789.02;
		
		account.setBalance(newBalance);
		
		assertEquals(newBalance, account.getBalance());
	}
	
	@Test
	public void account_setCustomer() throws Exception {
		
		Account account = new SavingsAccount(1234.56, person("Test", new Address()), 0.05);
		
		Address newAddress = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
		Customer newCustomer = person("John Doe", newAddress);
		
		account.setCustomer(newCustomer);
		
		assertEquals(newCustomer, account.getCustomer());
	}
	
	@Test
	public void checkingAccount_setNextCheckNumber() {
		
		CheckingAccount account = new CheckingAccount(6789.01, company("Test", new Address()), 10);
		int nextCheckNumber = 20;
		
		account.setNextCheckNumber(nextCheckNumber);
		
		assertEquals(nextCheckNumber, account.getNextCheckNumber());
	}
	
	@Test
	public void savingsAccount_setInterestRate() {
		
		SavingsAccount account = new SavingsAccount(84287.63, company("Test", new Address()), 0.05);
		double newInterestRate = 0.08;
		
		account.setInterestRate(newInterestRate);
		
		assertEquals(newInterestRate, account.getInterestRate());
	}
	
	@Test
	public void customer_setAccounts() {
		
		Account account1 = new SavingsAccount(1234.56, person("Test", new Address()), 0.05);
		Account account2 = new CheckingAccount(6789.01, company("Test", new Address()), 10);
		List<Account> accounts = List.of(account1, account2);
		
		Customer customer = person("John Doe", new Address());
		
		customer.setAccounts(accounts);
		
		assertEquals(accounts, customer.getAccounts());
	}

	@Test
	public void findAccountByCity_ifPresent_returnsAccounts() throws Exception {
		
		String city = "Toronto";
		
		Address address = new Address("130 Adelaide St West", "M5H 3P5");
        Customer customer = company("FDM Group", address);
        
        Account account = new SavingsAccount(1234.56, customer, 0.05);
		List<Account> expectedAccounts = List.of(account);
		customer.setAccounts(expectedAccounts);
		
		Account otherAccount = new SavingsAccount(1234.56, person("Test", new Address()), 0.05);
		Address otherAddress = new Address("300 Barangaroo Avenue", "2000", "Sydney", "New South Wales");
		Customer otherCustomer = person("John Doe", otherAddress);
		otherAccount.setCustomer(otherCustomer);
		
		List<Account> fullListOfAccounts = List.of(account, otherAccount);

        when(accountRepo.findAll()).thenReturn(fullListOfAccounts);

        List<Account> actualAccounts = accountService.findByCity(city);

        assertEquals(expectedAccounts, actualAccounts);
    } 
	
	@Test
	public void saveAccount_WithCustomer_savesAddressCustomerAndAccount() throws Exception {
		
		Address address = new Address("130 Adelaide St West", "M5H 3P5");
        Customer customer = company("FDM Group", address);
     
        Account account = new SavingsAccount(1234.56, customer, 0.05);
        
        accountService.saveAccount(account);
        
        verify(addressRepo).save(address);
        verify(customerRepo).save(customer);
        verify(accountRepo).save(account);
	}
	
	@Test
	public void saveAccount_WithoutCustomer_savesOnlyAccount() throws Exception {
     
        Account account = new SavingsAccount(1234.56, null, 0.05);
        
        accountService.saveAccount(account);
        
        verify(accountRepo).save(account);
        verify(addressRepo, never()).save(any());
        verify(customerRepo, never()).save(any());
	}
	
	@Test
	public void getAll_returns200() throws Exception {
		
		mvc.perform(MockMvcRequestBuilders.get("/account/getAll"))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	void setCustomer_WhenAccountAndCustomerExist_UpdatesAccount() throws Exception {
	    
		Customer customer = mock(Customer.class);
     
        Account account = mock(Account.class);

	    when(accountRepo.findById(1L))
	            .thenReturn(Optional.of(account));

	    when(customerRepo.findById(2L))
	            .thenReturn(Optional.of(customer));

	    when(customer.getCustomerId())
	            .thenReturn(2L);

	    accountService.setCustomer(1L, 2L);

	    verify(account).setCustomer(customer);
	    verify(account).setCustomerId(2L);
	    verify(accountRepo).save(account);
	}
	
	@Test
	void setCustomer_WhenAccountDoesNotExist_DoesNotSave() {
		
	    when(accountRepo.findById(1L))
	            .thenReturn(Optional.empty());

	    when(customerRepo.findById(2L))
	            .thenReturn(Optional.of(mock(Customer.class)));

	    accountService.setCustomer(1L, 2L);

	    verify(accountRepo, never()).save(any(Account.class));
	}
	
	@Test
	void setCustomer_WhenCustomerDoesNotExist_DoesNotSave() {
		
	    when(accountRepo.findById(1L))
	            .thenReturn(Optional.of(mock(Account.class)));

	    when(customerRepo.findById(2L))
	            .thenReturn(Optional.empty());

	    accountService.setCustomer(1L, 2L);

	    verify(accountRepo, never()).save(any(Account.class));
	}
	
	@Test
	void addAccount_ExistingAccounts_AssignsCustomerAndSavesAccount() throws Exception {
		Customer customer = company("Test", new Address());
		Account existingAccount = new SavingsAccount(50.0, customer, 0.02);
		customer.setAccounts(new ArrayList<>(List.of(existingAccount)));
		Account account = new SavingsAccount(1234.56, null, 0.05);

		when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
		when(accountRepo.findById(2L)).thenReturn(Optional.of(account));

		customerService.addAccount(1L, 2L);

		assertEquals(customer, account.getCustomer());
		assertEquals(List.of(existingAccount), customer.getAccounts());
		verify(accountRepo).save(account);
		verify(customerRepo, never()).save(customer);
	}
	
	@Test
	void addAccount_NullAccounts_AssignsCustomerAndSavesAccount() throws Exception {
		Customer customer = company("Test", new Address());
		customer.setAccounts(null);
		Account account = new SavingsAccount(1234.56, null, 0.05);

		when(customerRepo.findById(1L)).thenReturn(Optional.of(customer));
		when(accountRepo.findById(2L)).thenReturn(Optional.of(account));

		customerService.addAccount(1L, 2L);

		assertNotNull(customer.getAccounts());
		assertEquals(customer, account.getCustomer());
		verify(accountRepo).save(account);
	}
	
	@Test
	void addAccount_MissingCustomerOrAccount_ThrowsWithoutSaving() {
	    when(customerRepo.findById(1L))
	            .thenReturn(Optional.empty(), Optional.of(company("Test", new Address())));

	    assertThrows(IllegalArgumentException.class, () -> customerService.addAccount(1L, 2L));
	    assertThrows(IllegalArgumentException.class, () -> customerService.addAccount(1L, 2L));

	    verify(accountRepo).findById(2L);
	    verify(accountRepo, never()).save(any());
	    verify(customerRepo, never()).save(any());
	}
	
	@Test
	void addAccount_assignsCustomerToEachAccount() {
	    Customer customer = company("Test", new Address());
	    Account account1 = new SavingsAccount(100.0, null, 0.05);
	    Account account2 = new CheckingAccount(200.0, null, 10);

	    when(customerRepo.findById(3L)).thenReturn(Optional.of(customer));
	    when(accountRepo.findById(1L)).thenReturn(Optional.of(account1));
	    when(accountRepo.findById(2L)).thenReturn(Optional.of(account2));

	    customerService.addAccount(3L, 1L);
	    customerService.addAccount(3L, 2L);

	    assertEquals(customer, account1.getCustomer());
	    assertEquals(customer, account2.getCustomer());
	    verify(accountRepo).save(account1);
	    verify(accountRepo).save(account2);
	}
	
	@Test
	public void setCustomer_ifNull_DoNothing() {
		
	    SavingsAccount account = new SavingsAccount(1234.56, null, 0.05);

	    account.setCustomer(null);

	    assertNull(account.getCustomer());
	}
	
	@Test
	public void setCustomerId_ifCustomerNull_DoNothing() {
		
		Account account = new SavingsAccount(1234.56, null, 0.05);
		
		long actualCustomerId = account.getCustomerId();
		
		assertEquals(0, actualCustomerId);
	}
	
	@Test
	void findByCity_returnsAccountsInCity() throws Exception {

	    Address address1 = new Address("152 Fanshawe Street", "1010", "Auckland", "Auckland");
	    Customer customer1 = company("FDM Group", address1);
	    
	    Address address2 = new Address("697 Collins Street", "3000", "Melbourne", "Victoria");
	    Customer customer2 = company("FDM Group", address2);

	    Account account1 = new SavingsAccount(1234.56, customer1, 0.05);
	    Account account2 = new CheckingAccount(6789.01, customer2, 10);

	    List<Account> accounts = List.of(account1, account2);

	    when(accountRepo.findAll()).thenReturn(accounts);

	    List<Account> result = accountService.findByCity("Auckland");

	    assertEquals(1, result.size());
	    assertEquals(account1, result.get(0));
	}
	
	@Test
	public void getAccountsByCity_IfNoAccounts_ReturnEmpty() throws Exception {
		
		List<Account> actualList = accountService.findByCity("");
		
		assertEquals(new ArrayList<>(), actualList);
	}
	
	@Test
	void setCustomer_returns200() throws Exception {
		AccountService service = mock(AccountService.class);
		mvc = accountControllerMvc(service);
	    mvc.perform(MockMvcRequestBuilders.put("/account/setCustomer")
	       .param("accountId", "1")
	       .param("customerId", "2"))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	void address_incorrectCityOrProvince_throwsException() {
	    assertThrows(IncorrectCityOrProvinceException.class,
	    			() -> new Address("152 Fanshawe Street", "1010", "Wellington", "Wellington")
	    );
	}
	
	@Test
	void getByCity_returns200() throws Exception {

	    mvc.perform(MockMvcRequestBuilders.get("/account/getByCity")
	            .param("city", "Auckland"))
	            .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void editCheckingAccount_returns200() throws Exception {
		
		Long id = 1L;
		CheckingAccount newAccount = new CheckingAccount(6789.01, null, 10);
		newAccount.setAccountId(id);

	    String accountJson = objectMapper.writeValueAsString(newAccount);

		AccountService service = mock(AccountService.class);
		when(service.saveAccount(any(Account.class))).thenReturn(newAccount);
		mvc = accountControllerMvc(service);
	    mvc.perform(MockMvcRequestBuilders.put("/account/edit")
	            						  .contentType(MediaType.APPLICATION_JSON)
	            						  .content(accountJson))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void deleteAccount_returns200() throws Exception {
		
		Long id = 60L;
		
		mvc.perform(MockMvcRequestBuilders.delete("/account/delete")
	            						  .param("accountId", Long.toString(id)))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void deleteAccount_callsRepo_deletesAccount() {

		Long id = 1L;
		
	    Account account = new SavingsAccount();
	    account.setAccountId(id);

	    when(accountRepo.findById(id)).thenReturn(Optional.of(account));

	    accountService.deleteAccount(id);

	    verify(accountRepo).findById(id);
	    verify(accountRepo).delete(account);
	}
	
	@Test
	public void findAccountById_returns200() throws Exception {
		
		Long id = 1L;
		
		mvc.perform(MockMvcRequestBuilders.get("/account/get")
	            						  .param("accountId", Long.toString(id)))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	public void findAccountById_ifPresent_returnsAccount() {
		
		Long id = 1L;

        Account expectedAccount = new SavingsAccount();
        expectedAccount.setAccountId(id);

        when(accountRepo.findById(id))
            .thenReturn(Optional.of(expectedAccount));

        Account actualAccount = accountService.findAccountById(id);

        assertEquals(expectedAccount, actualAccount);
    } 
	
	@Test
	public void findAccountById_ifNotPresent_returnsNull() {
		
		Long id = 1L;

        when(accountRepo.findById(id))
            .thenReturn(Optional.empty());

        Account actualAccount = accountService.findAccountById(id);

        assertEquals(null, actualAccount);
    } 
	
	@Test
	public void getAccounts_returns200() throws Exception {
		
		Long id = 1L;
		
		mvc.perform(MockMvcRequestBuilders.get("/customer/getAccounts")
	            						  .param("customerId", Long.toString(id)))
	       .andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
    void getAccounts_returnsAccounts_whenCustomerExists() {
        Customer customer = person("Test", new Address());

        List<Account> accounts = List.of(
                new SavingsAccount(1000.0, customer, 0.05),
                new CheckingAccount(500.0, customer, 34)
        );

        customer.setAccounts(accounts);

        when(customerRepo.findById(1L))
                .thenReturn(Optional.of(customer));

        List<Account> result = customerService.getAccounts(1L);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(accounts, result);

        verify(customerRepo).findById(1L);
    }

    @Test
    void getAccounts_returnsEmpty_whenCustomerNotFound() {
        when(customerRepo.findById(1L))
                .thenReturn(Optional.empty());

        List<Account> result = customerService.getAccounts(1L);

        assertTrue(result.isEmpty());

        verify(customerRepo).findById(1L);
    }
}
