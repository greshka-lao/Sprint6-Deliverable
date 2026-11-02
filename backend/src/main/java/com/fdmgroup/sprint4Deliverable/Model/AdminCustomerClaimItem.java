package com.fdmgroup.sprint4Deliverable.Model;

import java.time.Instant;

public record AdminCustomerClaimItem(
		long claimRequestId,
		String customerName,
		String customerType,
		String address,
		String submittedEmail,
		String submittedPhoneNumber,
		Instant submittedAt) {
}
