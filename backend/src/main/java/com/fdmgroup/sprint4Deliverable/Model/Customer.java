package com.fdmgroup.sprint4Deliverable.Model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.Transient;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "customerType"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Person.class, name = "person"),
    @JsonSubTypes.Type(value = Company.class, name = "company")
})
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Customer {

	@Id
	@Column(name="customer_id")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long customerId;
	
	@Column(name="customer_name")
	private String name;
	
	@OneToOne
	@JoinColumn(name = "fk_address_id", referencedColumnName = "address_id")
	private Address address;
	
	@OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnore
	private List<Account> accounts;
	
	@Transient
	@Schema(
	    description = "The IDs of accounts assigned to this customer"
	)
	private List<Long> accountIds;
	
	public Customer() {
		super();
	}

	public Customer(String name, Address address, List<Account> accounts) {
		super();
		this.name = name;
		this.address = address;
		this.accounts = accounts;
		accountIds = new ArrayList<>();
		if (accounts != null) accounts.forEach(a -> accountIds.add(a.getAccountId()));
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

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}
	
	public void setAccounts(List<Account> accounts) {
		this.accounts = accounts;
		setAccountIds(accounts);
	}

	public List<Account> getAccounts() {
		return accounts;
	}
	
	public void setAccountIds(List<Account> accounts) {
		if (accountIds == null) accountIds = new ArrayList<>();
		accounts.forEach(a -> accountIds.add(a.getAccountId()));
	}
	
	public List<Long> getAccountIds() {
		List<Long> accountIds = new ArrayList<>();
		accounts.forEach(a -> accountIds.add(a.getAccountId()));
		return accountIds;
	}
}
