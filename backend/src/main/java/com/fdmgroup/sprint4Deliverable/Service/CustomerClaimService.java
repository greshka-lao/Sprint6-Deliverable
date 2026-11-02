package com.fdmgroup.sprint4Deliverable.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Model.AdminCustomerClaimItem;
import com.fdmgroup.sprint4Deliverable.Model.Customer;
import com.fdmgroup.sprint4Deliverable.Model.CustomerClaimCompletionResponse;
import com.fdmgroup.sprint4Deliverable.Model.CustomerClaimRequest;
import com.fdmgroup.sprint4Deliverable.Model.CustomerClaimStatusResponse;
import com.fdmgroup.sprint4Deliverable.Model.CustomerClaimSubmissionResponse;
import com.fdmgroup.sprint4Deliverable.Model.CustomerRegistrationRequest.AddressRequest;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerClaimRequestRepository;
import com.fdmgroup.sprint4Deliverable.Repository.CustomerRepository;

@Service
public class CustomerClaimService {

	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");
	private static final int PASSWORD_MIN_LENGTH = 8;
	private static final int PASSWORD_MAX_LENGTH = 1024;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final CustomerRepository customerRepository;
	private final CustomerClaimRequestRepository claimRepository;
	private final CustomerUsernameService usernameService;
	private final PasswordEncoder passwordEncoder;

	public CustomerClaimService(CustomerRepository customerRepository,
			CustomerClaimRequestRepository claimRepository,
			CustomerUsernameService usernameService,
			@Qualifier("customerPasswordEncoder")
			PasswordEncoder passwordEncoder) {
		this.customerRepository = customerRepository;
		this.claimRepository = claimRepository;
		this.usernameService = usernameService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public CustomerClaimSubmissionResponse submit(String name, String customerType, AddressRequest address,
			String email, String phoneNumber) {
		ValidatedClaim claim = validate(name, customerType, address, email, phoneNumber);
		List<Customer> exactMatches = customerRepository.findByNameAndCustomerType(
						claim.name(), claim.customerType()).stream()
				.filter(customer -> claim.name().equals(customer.getName())
						&& claim.customerType().equals(customer.getCustomerType())
						&& exactAddress(customer.getAddress(), claim.address()))
				.toList();
		if (exactMatches.size() != 1) {
			return unavailable();
		}

		Customer customer = exactMatches.get(0);
		if (!"ACTIVE".equals(customer.getStatus())
				|| customer.getUsername() != null || customer.getEmail() != null
				|| customer.getPasswordHash() != null
				|| customerRepository.existsByEmail(claim.email())
				|| claimRepository.existsByCustomer_CustomerIdAndState(
						customer.getCustomerId(), "PENDING")) {
			return unavailable();
		}

		String reference = newReference();
		CustomerClaimRequest request = new CustomerClaimRequest(
				customer, hashReference(reference), claim.email(), claim.phoneNumber());
		claimRepository.save(request);
		return new CustomerClaimSubmissionResponse("PENDING", reference,
				"Your claim request is awaiting administrator review.");
	}

	@Transactional(readOnly = true)
	public CustomerClaimStatusResponse status(String reference) {
		CustomerClaimRequest request = findByReference(reference);
		if ("COMPLETED".equals(request.getState())) {
			throw new InvalidClaimReferenceException();
		}
		return new CustomerClaimStatusResponse(request.getState());
	}

	@Transactional
	public CustomerClaimCompletionResponse complete(String reference, String password) {
		if (password == null || password.length() < PASSWORD_MIN_LENGTH
				|| password.length() > PASSWORD_MAX_LENGTH) {
			throw new ClaimValidationException();
		}
		CustomerClaimRequest request = findByReference(reference);
		if (!"APPROVED".equals(request.getState())) {
			throw new InvalidClaimReferenceException();
		}

		Customer customer = request.getCustomer();
		if (!"ACTIVE".equals(customer.getStatus())
				|| customer.getUsername() != null || customer.getEmail() != null
				|| customer.getPasswordHash() != null
				|| customerRepository.existsByEmail(request.getSubmittedEmail())) {
			throw new InvalidClaimReferenceException();
		}

		String base = usernameService.baseFor(customer.getName());
		int sequence = usernameService.nextAvailableSequence(base);
		customer.setUsername(usernameService.candidate(base, sequence));
		customer.setEmail(request.getSubmittedEmail());
		customer.setPhoneNumber(request.getSubmittedPhoneNumber());
		customer.setPasswordHash(passwordEncoder.encode(password));
		request.complete();
		customerRepository.saveAndFlush(customer);
		claimRepository.save(request);
		return new CustomerClaimCompletionResponse(customer.getCustomerId(), customer.getName(),
				customer.getUsername(), "Your profile is ready. Please sign in.");
	}

	@Transactional(readOnly = true)
	public List<AdminCustomerClaimItem> pendingClaims() {
		return claimRepository.findByStateOrderBySubmittedAtAsc("PENDING").stream()
				.map(request -> {
					Customer customer = request.getCustomer();
					return new AdminCustomerClaimItem(request.getClaimRequestId(), customer.getName(),
							customer.getCustomerType(), addressText(customer.getAddress()),
							request.getSubmittedEmail(), request.getSubmittedPhoneNumber(),
							request.getSubmittedAt());
				}).toList();
	}

	@Transactional
	public void approve(long claimRequestId, String reviewer) {
		CustomerClaimRequest request = claimRepository.findById(claimRequestId)
				.orElseThrow(ClaimRequestNotFoundException::new);
		request.approve(reviewer);
		claimRepository.save(request);
	}

	@Transactional
	public void reject(long claimRequestId, String reviewer) {
		CustomerClaimRequest request = claimRepository.findById(claimRequestId)
				.orElseThrow(ClaimRequestNotFoundException::new);
		request.reject(reviewer);
		claimRepository.save(request);
	}

	private CustomerClaimRequest findByReference(String reference) {
		if (reference == null || reference.isBlank()) {
			throw new InvalidClaimReferenceException();
		}
		return claimRepository.findByClaimReferenceHash(hashReference(reference))
				.orElseThrow(InvalidClaimReferenceException::new);
	}

	private ValidatedClaim validate(String name, String customerType, AddressRequest address,
			String email, String phoneNumber) {
		String normalizedName = name == null ? "" : name.trim();
		String normalizedType = customerType == null ? "" : customerType.trim().toLowerCase(Locale.ROOT);
		String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
		String normalizedPhone = phoneNumber == null ? "" : phoneNumber.trim();
		if (address == null || normalizedName.isEmpty() || normalizedName.length() > 200
				|| (!"person".equals(normalizedType) && !"company".equals(normalizedType))
				|| normalizedEmail.length() > 320 || !EMAIL.matcher(normalizedEmail).matches()
				|| !E164.matcher(normalizedPhone).matches()
				|| isIncomplete(address.streetNumber()) || isIncomplete(address.postalCode())
				|| isIncomplete(address.city()) || isIncomplete(address.province())) {
			throw new ClaimValidationException();
		}
		Address normalizedAddress = new Address();
		normalizedAddress.setStreetNumber(address.streetNumber().trim());
		normalizedAddress.setPostalCode(address.postalCode().trim());
		normalizedAddress.setCity(address.city().trim());
		normalizedAddress.setProvince(address.province().trim());
		return new ValidatedClaim(normalizedName, normalizedType, normalizedAddress,
				normalizedEmail, normalizedPhone);
	}

	private boolean exactAddress(Address candidate, Address requested) {
		return candidate != null
				&& equal(candidate.getStreetNumber(), requested.getStreetNumber())
				&& equal(candidate.getPostalCode(), requested.getPostalCode())
				&& equal(candidate.getCity(), requested.getCity())
				&& equal(candidate.getProvince(), requested.getProvince());
	}

	private static boolean equal(String left, String right) {
		return left != null && left.equals(right);
	}

	private static boolean isIncomplete(String value) {
		return value == null || value.isBlank();
	}

	private static String newReference() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String hashReference(String reference) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(reference.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}

	private static String addressText(Address address) {
		return address == null ? "" : java.util.stream.Stream.of(
				address.getStreetNumber(), address.getCity(), address.getProvince(), address.getPostalCode())
				.filter(value -> value != null && !value.isBlank())
				.collect(java.util.stream.Collectors.joining(", "));
	}

	private static CustomerClaimSubmissionResponse unavailable() {
		return new CustomerClaimSubmissionResponse("UNAVAILABLE", null,
				"If the details match an eligible profile, your request will be reviewed.");
	}

	private record ValidatedClaim(String name, String customerType, Address address,
			String email, String phoneNumber) {
	}

	public static class ClaimValidationException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static class InvalidClaimReferenceException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

	public static class ClaimRequestNotFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
}
