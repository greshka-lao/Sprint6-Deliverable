package com.fdmgroup.sprint4Deliverable.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fdmgroup.sprint4Deliverable.Repository.CustomerAccountRepository;

@ExtendWith(MockitoExtension.class)
class CustomerUsernameServiceTests {

	@Mock
	private CustomerAccountRepository repository;

	@InjectMocks
	private CustomerUsernameService usernameService;

	@Test
	void usernameBaseLowercasesNameAndRemovesWhitespace() {
		assertEquals("greshkalao", usernameService.baseFor("Greshka Lao"));
	}

	@Test
	void suffixIsPaddedToAtLeastThreeDigitsAndExpandsAfter999() {
		assertEquals("greshkalao001", usernameService.candidate("greshkalao", 1));
		assertEquals("greshkalao1000", usernameService.candidate("greshkalao", 1000));
	}

	@Test
	void nextAvailableSequenceSkipsExistingUsernames() {
		when(repository.existsByUsername("greshkalao001")).thenReturn(true);
		when(repository.existsByUsername("greshkalao002")).thenReturn(true);
		when(repository.existsByUsername("greshkalao003")).thenReturn(false);

		assertEquals(3, usernameService.nextAvailableSequence("greshkalao"));
	}
}
