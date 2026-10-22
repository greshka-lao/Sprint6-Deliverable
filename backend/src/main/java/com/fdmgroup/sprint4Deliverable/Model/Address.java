package com.fdmgroup.sprint4Deliverable.Model;

import com.fdmgroup.sprint4Deliverable.Exception.IncorrectCityOrProvinceException;
import com.fdmgroup.sprint4Deliverable.Util.Location;
import com.fdmgroup.sprint4Deliverable.Util.PostalCodeLookup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Address {

	@Id
	@Column(name="address_id")
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private long addressId;
	
	@Column(name="street_number")
	private String streetNumber;
	
	@Column(name="postal_code")
	private String postalCode;
	
	private String city;
	
	private String province;
	
	public Address() {
		super();
	}
	
	public Address(String streetNumber, String postalCode) throws Exception {
		super();
		this.streetNumber = streetNumber;
		this.postalCode = postalCode;
		
		Location location = PostalCodeLookup.findLocation(postalCode);
		this.city = location.city();
		this.province = location.province();
	}

	public Address(String streetNumber, String postalCode, String city, String province) throws Exception {
		super();
		this.streetNumber = streetNumber;
		this.postalCode = postalCode;
		
		// Check if city and province entered match with PostalCodeLookup map
		
		Location location = PostalCodeLookup.findLocation(postalCode);
			
		if (city.equals(location.city()) && province.equals(location.province())) {
			
			this.city = location.city();
			this.province = location.province();
			
		} else {
			throw new IncorrectCityOrProvinceException();
		}
	}

	public long getAddressId() {
		return addressId;
	}

	public void setAddressId(long addressId) {
		this.addressId = addressId;
	}

	public String getStreetNumber() {
		return streetNumber;
	}

	public void setStreetNumber(String streetNumber) {
		this.streetNumber = streetNumber;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}
}
