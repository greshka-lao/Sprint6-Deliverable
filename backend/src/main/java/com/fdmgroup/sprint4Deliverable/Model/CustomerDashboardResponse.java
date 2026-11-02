package com.fdmgroup.sprint4Deliverable.Model;

import java.math.BigDecimal;
import java.util.List;

public record CustomerDashboardResponse(
		CustomerProfile profile,
		List<CustomerAccount> accounts,
		AccountTotal total) {

	public record CustomerProfile(
			String name,
			String email,
			String phoneNumber,
			PostalAddress address) {
	}

	public record PostalAddress(
			String streetNumber,
			String city,
			String province,
			String postalCode) {
	}

	public record CustomerAccount(
			long accountId,
			String accountType,
			BigDecimal balance,
			String currency) {
	}

	public record AccountTotal(
			BigDecimal amount,
			String currency) {
	}
}
