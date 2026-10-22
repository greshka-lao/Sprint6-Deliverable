package com.fdmgroup.sprint4Deliverable.Service;

import org.springframework.stereotype.Service;

import com.fdmgroup.sprint4Deliverable.Exception.IncorrectCityOrProvinceException;
import com.fdmgroup.sprint4Deliverable.Model.Address;
import com.fdmgroup.sprint4Deliverable.Repository.AddressRepository;
import com.fdmgroup.sprint4Deliverable.Util.Location;
import com.fdmgroup.sprint4Deliverable.Util.PostalCodeLookup;

@Service
public class AddressService {

	private AddressRepository addressRepo;

	public AddressService(AddressRepository addressRepo) {
		super();
		this.addressRepo = addressRepo;
	}
	
	public void saveAddress(Address address) throws Exception {
		
		Location location = PostalCodeLookup.findLocation(address.getPostalCode());
		
		if ((!address.getCity().isBlank() && !location.city().equals(address.getCity())) || (!address.getProvince().isBlank() && !location.province().equals(address.getProvince()))) {
			throw new IncorrectCityOrProvinceException();
		}
		
		address.setCity(location.city());	
		address.setProvince(location.province());
		
		addressRepo.save(address);
	}
}
