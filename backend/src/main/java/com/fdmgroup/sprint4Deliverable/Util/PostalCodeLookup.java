package com.fdmgroup.sprint4Deliverable.Util;

import java.util.Map;

import com.fdmgroup.sprint4Deliverable.Exception.PostalCodeNotFoundException;

public final class PostalCodeLookup {

	private static final Map<String, Location> POSTAL_CODES = Map.ofEntries(
		Map.entry("1010", new Location("Auckland", "Auckland")),
		Map.entry("6011", new Location("Wellington", "Wellington")),
		Map.entry("8011", new Location("Christchurch", "Canterbury")),
		Map.entry("9010", new Location("Dunedin", "Otago")), 
		Map.entry("3216", new Location("Hamilton", "Waikato")),
		Map.entry("3110", new Location("Tauranga", "Bay of Plenty")),
		Map.entry("4110", new Location("Napier", "Hawke's Bay")),
		Map.entry("7010", new Location("Nelson", "Nelson")),
		Map.entry("4410", new Location("Palmerston North", "Manawatu-Whanganui")),
		Map.entry("0110", new Location("Whangarei", "Northland")),

		Map.entry("2000", new Location("Sydney", "New South Wales")),
		Map.entry("3000", new Location("Melbourne", "Victoria")),
		Map.entry("4000", new Location("Brisbane", "Queensland")),
		Map.entry("5000", new Location("Adelaide", "South Australia")),
		Map.entry("6000", new Location("Perth", "Western Australia")),
		Map.entry("7000", new Location("Hobart", "Tasmania")),
		Map.entry("0800", new Location("Darwin", "Northern Territory")),
		Map.entry("2600", new Location("Canberra", "Australian Capital Territory")),
		Map.entry("2300", new Location("Newcastle", "New South Wales")),
		Map.entry("3220", new Location("Geelong", "Victoria")),

		Map.entry("M5H 3P5", new Location("Toronto", "Ontario")),
		Map.entry("V6B 2W9", new Location("Vancouver", "British Columbia")),
		Map.entry("H2Y 1C6", new Location("Montreal", "Quebec")),
		Map.entry("T2P 1J9", new Location("Calgary", "Alberta")),
		Map.entry("R3C 4T3", new Location("Winnipeg", "Manitoba"))
	);

	public static Location findLocation(String postalCode) throws Exception {

		Location location = POSTAL_CODES.get(postalCode);

		if (location == null) {
			throw new PostalCodeNotFoundException(postalCode);
		}

		return location;
	}
}
