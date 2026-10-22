package com.fdmgroup.sprint4Deliverable.Model;

import jakarta.persistence.Entity;

@Entity	
public class CheckingAccount extends Account {
	
	private int nextCheckNumber;

	public CheckingAccount() {
		super();
	}

	public CheckingAccount(double balance, Customer customer, int nextCheckNumber) {
		super(balance, customer);
		this.nextCheckNumber = nextCheckNumber;
	}

	public int getNextCheckNumber() {
		return nextCheckNumber;
	}

	public void setNextCheckNumber(int nextCheckNumber) {
		this.nextCheckNumber = nextCheckNumber;
	}
}
