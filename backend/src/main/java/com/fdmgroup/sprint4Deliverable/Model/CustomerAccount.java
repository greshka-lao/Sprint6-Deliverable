package com.fdmgroup.sprint4Deliverable.Model;

import java.time.Instant;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_accounts")
public class CustomerAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, unique = true, length = 260)
	private String username;

	@OneToOne(cascade = CascadeType.ALL, optional = false)
	@JoinColumn(name = "address_id", nullable = false, unique = true)
	private Address address;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(nullable = false, length = 16)
	private String phoneNumber;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@Column(nullable = false, length = 32)
	private String role = "CUSTOMER";

	@Column(nullable = false, length = 32)
	private String status = "ACTIVE";

	@Column(nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	private Instant lastLoginAt;

	protected CustomerAccount() {
	}

	public CustomerAccount(String name, String username, Address address, String email,
			String phoneNumber, String passwordHash) {
		this.name = name;
		this.username = username;
		this.address = address;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.passwordHash = passwordHash;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getUsername() {
		return username;
	}

	public Address getAddress() {
		return address;
	}

	public String getEmail() {
		return email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getRole() {
		return role;
	}

	public String getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getLastLoginAt() {
		return lastLoginAt;
	}

	public void recordSuccessfulLogin() {
		this.lastLoginAt = Instant.now();
	}
}
