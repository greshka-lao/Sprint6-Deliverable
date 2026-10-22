package com.fdmgroup.sprint4Deliverable.Exception;

public class IncorrectCityOrProvinceException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IncorrectCityOrProvinceException() {
        super("Incorrect city or province entered");
    }
}
