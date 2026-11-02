package com.fdmgroup.sprint4Deliverable.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fdmgroup.sprint4Deliverable.Exception.IncorrectCityOrProvinceException;
import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.Company;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Model.CustomerAuthResponse;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationRequest;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationResponse;
import com.fdmgroup.sprint4Deliverable.Model.Person;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class CustomerAuthService {

	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");
	private static final int MAX_LOGIN_FAILURES = 5;
	private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
	private static final Duration RATE_LIMIT_DURATION = Duration.ofMinutes(15);
	private static final int USERNAME_RETRY_LIMIT = 5;

	private final CustomerRepository customerRepository;
	private final AddressRepository addressRepository;
	private final CustomerUsernameService usernameService;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final Map<String, FailureWindow> failedLogins = new ConcurrentHashMap<>();

	public CustomerAuthService(CustomerRepository customerRepository,
			AddressRepository addressRepository,
			CustomerUsernameService usernameService,
			@Qualifier("customerPasswordEncoder")
			PasswordEncoder customerPasswordEncoder,
			@Qualifier("customerAuthenticationManager")
			AuthenticationManager authenticationManager) {
		this.customerRepository = customerRepository;
		this.addressRepository = addressRepository;
		this.usernameService = usernameService;
		this.passwordEncoder = customerPasswordEncoder;
		this.authenticationManager = authenticationManager;
	}

	public CustomerRegistrationResponse register(CustomerRegistrationRequest request) {
		ValidatedRegistration registration = validateRegistration(request);
		String base = usernameService.baseFor(registration.name());
		int sequence = usernameService.nextAvailableSequence(base);
		boolean existingProfile = customerRepository.findByNameAndCustomerType(
						registration.name(), registration.customerType()).stream()
				.anyMatch(customer -> registration.name().equals(customer.getName())
						&& registration.customerType().equals(customer.getCustomerType())
						&& exactAddress(customer.getAddress(), registration.address()));
		if (existingProfile) {
			throw new ProfileClaimRequiredException();
		}

		for (int attempt = 0; attempt < USERNAME_RETRY_LIMIT; attempt++, sequence++) {
			String username = usernameService.candidate(base, sequence);
			
			Address address = registration.address();
			
			try {
				address = addressRepository.saveAndFlush(address);
			} catch (IncorrectCityOrProvinceException e) {
				throw e;
			}
			
			Customer customer = "person".equals(registration.customerType())
					? new Person(registration.name(), address, null)
					: new Company(registration.name(), address, null);
			customer.setUsername(username);
			customer.setEmail(registration.email());
			customer.setPhoneNumber(registration.phoneNumber());
			customer.setPasswordHash(passwordEncoder.encode(registration.password()));
			try {
				Customer savedCustomer = customerRepository.saveAndFlush(customer);
				return new CustomerRegistrationResponse(savedCustomer.getCustomerId(), savedCustomer.getName(), savedCustomer.getUsername(),
						savedCustomer.getEmail(), savedCustomer.getPhoneNumber(), "Registration successful. Please sign in.");
			} catch (DataIntegrityViolationException exception) {
				if (customerRepository.existsByEmail(registration.email())) {
					throw new DuplicateCustomerEmailException();
				}
				if (customerRepository.existsByUsername(username)) {
					continue;
				}
				throw exception;
			}
		}
		throw new IllegalStateException("Could not allocate a unique customer username after several attempts.");
	}

	public CustomerAuthResponse authenticate(String username, String password, HttpServletRequest request) {
		String normalizedUsername = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
		String rateKey = rateLimitKey(request, normalizedUsername);
		FailureWindow window = failedLogins.compute(rateKey, (key, current) -> current == null
				|| current.startedAt().plus(RATE_LIMIT_WINDOW).isBefore(Instant.now())
						? new FailureWindow(Instant.now(), 0, null)
						: current);
		if (window.blockedUntil() != null && window.blockedUntil().isAfter(Instant.now())) {
			throw new CustomerRateLimitException(Duration.between(Instant.now(), window.blockedUntil()).toSeconds() + 1);
		}

		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(normalizedUsername, password));
		} catch (AuthenticationException exception) {
			recordFailedLogin(rateKey);
			throw new BadCredentialsException("Invalid username or password.");
		}

		failedLogins.remove(rateKey);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		Customer account = customerRepository.findByUsername(authentication.getName())
				.orElseThrow(() -> new BadCredentialsException("Invalid username or password."));
		account.recordSuccessfulLogin();
		customerRepository.save(account);
		return new CustomerAuthResponse(account.getCustomerId(), account.getUsername(), account.getRole());
	}

	public CustomerAuthResponse customerForUsername(String username) {
		Customer account = customerRepository.findByUsername(username)
				.filter(customer -> "ACTIVE".equals(customer.getStatus())
						&& customer.getPasswordHash() != null && !customer.getPasswordHash().isBlank())
				.orElseThrow(() -> new BadCredentialsException("Customer account not found"));
		return new CustomerAuthResponse(account.getCustomerId(), account.getUsername(), account.getRole());
	}

	private void recordFailedLogin(String key) {
		failedLogins.compute(key, (ignored, previous) -> {
			Instant startedAt = previous == null ? Instant.now() : previous.startedAt();
			int failures = previous == null ? 1 : previous.failures() + 1;
			Instant blockedUntil = failures >= MAX_LOGIN_FAILURES ? Instant.now().plus(RATE_LIMIT_DURATION) : null;
			return new FailureWindow(startedAt, failures, blockedUntil);
		});
		if (failedLogins.size() > 10_000) {
			Instant cutoff = Instant.now().minus(RATE_LIMIT_WINDOW);
			failedLogins.entrySet().removeIf(entry -> entry.getValue().startedAt().isBefore(cutoff));
		}
	}

	private String rateLimitKey(HttpServletRequest request, String username) {
		String remoteAddress = request.getRemoteAddr();
		return remoteAddress + ":" + username;
	}

	private boolean exactAddress(Address candidate, Address requested) {
		return candidate != null
				&& java.util.Objects.equals(candidate.getStreetNumber(), requested.getStreetNumber())
				&& java.util.Objects.equals(candidate.getPostalCode(), requested.getPostalCode())
				&& java.util.Objects.equals(candidate.getCity(), requested.getCity())
				&& java.util.Objects.equals(candidate.getProvince(), requested.getProvince());
	}

	private ValidatedRegistration validateRegistration(CustomerRegistrationRequest request) {
		if (request == null || request.name() == null || request.address() == null
				|| request.email() == null || request.phoneNumber() == null || request.password() == null
				|| request.customerType() == null) {
			throw new RegistrationValidationException();
		}
		String name = request.name().trim();
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String phone = request.phoneNumber().trim();
		String customerType = request.customerType().trim().toLowerCase(Locale.ROOT);
		CustomerRegistrationRequest.AddressRequest suppliedAddress = request.address();
		String street = suppliedAddress.streetNumber() == null ? "" : suppliedAddress.streetNumber().trim();
		String postalCode = suppliedAddress.postalCode() == null ? "" : suppliedAddress.postalCode().trim();
		String city = suppliedAddress.city() == null ? "" : suppliedAddress.city().trim();
		String province = suppliedAddress.province() == null ? "" : suppliedAddress.province().trim();

		if (name.isEmpty() || name.length() > 200 || street.isEmpty() || street.length() > 200
				|| postalCode.isEmpty() || postalCode.length() > 32 || city.isEmpty() || city.length() > 120
				|| province.isEmpty() || province.length() > 120
				|| email.length() > 320 || !EMAIL.matcher(email).matches()
				|| !E164.matcher(phone).matches()
				|| (!"person".equals(customerType) && !"company".equals(customerType))
				|| request.password().length() < 8 || request.password().length() > 1024) {
			throw new RegistrationValidationException();
		}

		Address address = new Address();
		address.setStreetNumber(street);
		address.setPostalCode(postalCode);
		address.setCity(city);
		address.setProvince(province);
		return new ValidatedRegistration(name, address, email, phone, request.password(), customerType);
	}

	private record ValidatedRegistration(String name, Address address, String email,
			String phoneNumber, String password, String customerType) {
	}

	private record FailureWindow(Instant startedAt, int failures, Instant blockedUntil) {
	}

	public static class RegistrationValidationException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static class DuplicateCustomerEmailException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static class ProfileClaimRequiredException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static class CustomerRateLimitException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final long retryAfterSeconds;

		public CustomerRateLimitException(long retryAfterSeconds) {
			super("Sign-in is temporarily unavailable. Please try again later.");
			this.retryAfterSeconds = retryAfterSeconds;
		}

		public long getRetryAfterSeconds() {
			return retryAfterSeconds;
		}
	}
}
