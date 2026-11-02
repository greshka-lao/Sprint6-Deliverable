package com.fdmgroup.sprint4Deliverable.Model;

public record CustomerClaimSubmissionRequest(
		String name,
		String customerType,
		CustomerRegistrationRequest.AddressRequest address,
		String email,
		String phoneNumber) {
}
