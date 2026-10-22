package com.fdmgroup.sprint4Deliverable.Model;

import jakarta.persistence.Entity;

@Entity
public class SavingsAccount extends Account {

	private double interestRate;

	public SavingsAccount() {
		super();
	}

	public SavingsAccount(double balance, Customer customer, double interestRate) {
		super(balance, customer);
		this.interestRate = interestRate;
	}

	public double getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(double interestRate) {
		this.interestRate = interestRate;
	}
}
