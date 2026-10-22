package com.fdmgroup.sprint4Deliverable.Model;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "accountType")
@JsonSubTypes({ @JsonSubTypes.Type(value = SavingsAccount.class, name = "savingsAccount"),
		@JsonSubTypes.Type(value = CheckingAccount.class, name = "checkingAccount") })
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Account {

	@Id
	@Column(name = "account_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long accountId;

	private double balance;

	@ManyToOne
	@JoinColumn(name = "fk_cust_id")
	@JsonIgnore
	private Customer customer;
	
	@Transient
	@Schema(
	    description = "The ID of customer assigned to this account"
	)
	private long customerId;

	public Account() {
		super();
	}

	public Account(double balance, Customer customer) {
		super();
		this.balance = balance;
		if (customer != null) {
			this.customer = customer;
			setCustomerId(customer.getCustomerId());
		}
	}

	public long getAccountId() {
		return accountId;
	}

	public void setAccountId(long accountId) {
		this.accountId = accountId;
	}

	public double getBalance() {
		return balance;
	}

	public void setBalance(double balance) {
		this.balance = balance;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
		if (customer != null) {
			setCustomerId(customer.getCustomerId());
		}
	}
	
	public long getCustomerId() {
		if (customer != null) {
			return customer.getCustomerId();
		}
		return 0;
	}
	
	public void setCustomerId(long customerId) {
		this.customerId = customerId;
	}
}
