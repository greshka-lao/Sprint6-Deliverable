package com.fdmgroup.sprint4Deliverable.Exception;

public class PostalCodeNotFoundException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PostalCodeNotFoundException(String postalCode) {
        super("Postal code not found");
    }
}
