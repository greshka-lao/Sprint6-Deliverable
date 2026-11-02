package com.fdmgroup.sprint4Deliverable.Model;

public record AdminCustomerListItem(
		long customerId,
		String name,
		String username,
		String address,
		String customerType,
		String phoneNumber,
		String email) {
}
