package com.fdmgroup.sprint4Deliverable.Model;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "person")
@PrimaryKeyJoinColumn(name = "customer_id", referencedColumnName = "customer_id")
public class Person extends Customer {

	public Person() {
		super();
	}

	public Person(String name, Address address, List<Account> accounts) {
		super("person", name, address, accounts);
	}
}
