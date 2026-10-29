package com.fdmgroup.sprint4Deliverable.Model;

public record CustomerSessionResponse(
		boolean authenticated,
		CustomerAuthResponse customer,
		String code,
		String message) {

	public static CustomerSessionResponse authenticated(CustomerAuthResponse customer) {
		return new CustomerSessionResponse(true, customer, null, null);
	}

	public static CustomerSessionResponse expired() {
		return new CustomerSessionResponse(false, null, "session_expired",
				"Your session has expired. Please sign in again.");
	}
}
