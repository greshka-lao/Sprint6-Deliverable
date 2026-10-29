package com.fdmgroup.sprint4Deliverable.Service;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.fdmgroup.sprint4Deliverable.Repository.CustomerAccountRepository;

@Service
public class CustomerUsernameService {

	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private final CustomerAccountRepository repository;

	public CustomerUsernameService(CustomerAccountRepository repository) {
		this.repository = repository;
	}

	public String baseFor(String name) {
		return WHITESPACE.matcher(name.toLowerCase(Locale.ROOT)).replaceAll("");
	}

	public String candidate(String base, int sequence) {
		return base + String.format(Locale.ROOT, "%03d", sequence);
	}

	public int nextAvailableSequence(String base) {
		int sequence = 1;
		while (repository.existsByUsername(candidate(base, sequence))) {
			sequence++;
		}
		return sequence;
	}
}
