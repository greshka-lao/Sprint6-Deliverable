package com.fdmgroup.sprint4Deliverable.Model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "customerType")
@JsonSubTypes({
		@JsonSubTypes.Type(value = Person.class, name = "person"),
		@JsonSubTypes.Type(value = Company.class, name = "company")
})
@Entity
@Table(name = "customer")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Customer {
	private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{1,14}$");

	@Id
	@Column(name = "customer_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long customerId;

	@Column(name = "customer_name", nullable = false, length = 200)
	private String name;

	@Column(name = "customer_type", nullable = false, updatable = false, length = 16)
	private String customerType;

	@OneToOne
	@JoinColumn(name = "fk_address_id", referencedColumnName = "address_id", unique = true)
	private Address address;

	@Column(unique = true, length = 260)
	private String username;

	@Column(unique = true, length = 320)
	private String email;

	@Column(name = "phone_number", length = 16)
	private String phoneNumber;

	@JsonIgnore
	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@JsonIgnore
	@Column(nullable = false, length = 32)
	private String role = "CUSTOMER";

	@Column(nullable = false, length = 32)
	private String status = "ACTIVE";

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	private Instant lastLoginAt;

	@OneToMany(mappedBy = "customer")
	@JsonIgnore
	private List<Account> accounts = new ArrayList<>();

	@Transient
	@Schema(description = "The IDs of accounts assigned to this customer")
	private List<Long> accountIds;

	protected Customer() {
	}

	protected Customer(String customerType, String name, Address address, List<Account> accounts) {
		this.customerType = normalizeType(customerType);
		this.name = name;
		this.address = address;
		this.accounts = accounts == null ? new ArrayList<>() : accounts;
		setAccountIds(this.accounts);
	}

	@PrePersist
	@PreUpdate
	private void validateProfileType() {
		String type = normalizeType(customerType);
		if (("person".equals(type) && !(this instanceof Person))
				|| ("company".equals(type) && !(this instanceof Company))) {
			throw new IllegalStateException("Customer type must match its joined profile subtype.");
		}
		customerType = type;
		if (email != null) {
			email = email.trim().toLowerCase(Locale.ROOT);
			if (email.isEmpty()) {
				email = null;
			}
		}
		if (phoneNumber != null) {
			phoneNumber = phoneNumber.trim();
			if (!phoneNumber.isEmpty() && !E164.matcher(phoneNumber).matches()) {
				throw new IllegalStateException("Phone number must use E.164 format.");
			}
		}
		if ((username == null) != (passwordHash == null)
				|| (username == null) != (email == null)) {
			throw new IllegalStateException("Customer login credentials must be attached together.");
		}
	}

	private static String normalizeType(String value) {
		if (value == null) {
			throw new IllegalArgumentException("Customer type must be person or company.");
		}
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		if (!"person".equals(normalized) && !"company".equals(normalized)) {
			throw new IllegalArgumentException("Customer type must be person or company.");
		}
		return normalized;
	}

	public long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(long customerId) {
		this.customerId = customerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCustomerType() {
		return customerType;
	}

	public void setCustomerType(String customerType) {
		if (this.customerType != null && !this.customerType.equalsIgnoreCase(customerType)) {
			throw new IllegalArgumentException("Customer type cannot be changed after creation.");
		}
		this.customerType = normalizeType(customerType);
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	@JsonIgnore
	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	@JsonIgnore
	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public void recordSuccessfulLogin() {
		this.lastLoginAt = Instant.now();
	}

	public List<Account> getAccounts() {
		return accounts;
	}

	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts == null ? new ArrayList<>() : accounts;
		setAccountIds(this.accounts);
	}

	public void setAccountIds(List<Account> accounts) {
		if (accountIds == null) {
			accountIds = new ArrayList<>();
		}
		if (accounts != null) {
			accounts.forEach(account -> accountIds.add(account.getAccountId()));
		}
	}

	public List<Long> getAccountIds() {
		return accounts == null ? List.of()
				: accounts.stream().map(Account::getAccountId).toList();
	}
}
