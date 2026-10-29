package com.fdmgroup.sprint4Deliverable.Model;

public record CustomerRegistrationRequest(
		String name,
		AddressRequest address,
		String email,
		String phoneNumber,
		String password) {

	public record AddressRequest(String streetNumber, String postalCode, String city, String province) {
	}
}
