package com.fdmgroup.sprint4Deliverable.Model;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "company")
@PrimaryKeyJoinColumn(name = "customer_id", referencedColumnName = "customer_id")
public class Company extends Customer {

	public Company() {
		super();
	}

	public Company(String name, Address address, List<Account> accounts) {
		super("company", name, address, accounts);
	}
}
