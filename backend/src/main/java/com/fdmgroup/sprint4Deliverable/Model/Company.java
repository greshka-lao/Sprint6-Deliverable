package com.fdmgroup.sprint4Deliverable.Model;

import java.util.List;

import jakarta.persistence.Entity;

@Entity
public class Company extends Customer {

	public Company() {
		super();
	}

	public Company(String name, Address address, List<Account> accounts) {
		super(name, address, accounts);
	}
}
