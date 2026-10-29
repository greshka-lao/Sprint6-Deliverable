package com.fdmgroup.sprint4Deliverable.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import com.fdmgroup.sprint4Deliverable.Model.Session;
import com.fdmgroup.sprint4Deliverable.Repository.SessionRepository;
import com.fdmgroup.sprint4Deliverable.Security.TokenDigest;

@ExtendWith(MockitoExtension.class)
class SessionTokenMigrationRunnerTests {

    @Mock
    private SessionRepository sessionRepository;

    @Test
    void hashesLegacyRawTokensAndLeavesExistingDigestsUnchanged() throws Exception {
        SessionTokenMigrationRunner runner = new SessionTokenMigrationRunner(sessionRepository);
        Session legacy = new Session();
        legacy.setTokenDigest("legacy-raw-token");
        Session current = new Session();
        current.setTokenDigest(TokenDigest.sha256("already-hashed"));
        when(sessionRepository.findAll()).thenReturn(List.of(legacy, current));

        runner.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(TokenDigest.sha256("legacy-raw-token"), legacy.getTokenDigest());
        assertEquals(TokenDigest.sha256("already-hashed"), current.getTokenDigest());
        verify(sessionRepository).save(legacy);
    }
}
