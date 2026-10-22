package com.fdmgroup.sprint4Deliverable.Model;

import java.util.List;

import jakarta.persistence.Entity;

@Entity
public class Person extends Customer {

	public Person() {
		super();
	}

	public Person(String name, Address address, List<Account> accounts) {
		super(name, address, accounts);
	}
}
