package com.fdmgroup.sprint4Deliverable.Model;

public record CustomerRegistrationResponse(
		Long id,
		String name,
		String username,
		String email,
		String phoneNumber,
		String message) {
}
