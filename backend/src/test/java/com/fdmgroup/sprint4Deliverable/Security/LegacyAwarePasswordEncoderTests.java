package com.fdmgroup.sprint4Deliverable.Security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LegacyAwarePasswordEncoderTests {

    private final LegacyAwarePasswordEncoder encoder = new LegacyAwarePasswordEncoder();

    @Test
    void legacyPasswordMatchesOnlyForVerificationAndRequiresUpgrade() {
        assertTrue(encoder.matches("legacy-value", "legacy-value"));
        assertFalse(encoder.matches("wrong-value", "legacy-value"));
        assertTrue(encoder.upgradeEncoding("legacy-value"));
    }

    @Test
    void encodedPasswordUsesAdaptiveDelegatingEncoder() {
        String encoded = encoder.encode("new-password");

        assertTrue(encoded.startsWith("{bcrypt}"));
        assertTrue(encoder.matches("new-password", encoded));
        assertFalse(encoder.matches("wrong-password", encoded));
        assertFalse(encoder.upgradeEncoding(encoded));
    }
}
