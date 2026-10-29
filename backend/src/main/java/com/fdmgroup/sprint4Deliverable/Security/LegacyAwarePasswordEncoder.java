package com.fdmgroup.sprint4Deliverable.Security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class LegacyAwarePasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder delegatingEncoder =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return delegatingEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (encodedPassword.startsWith("{")) {
            return delegatingEncoder.matches(rawPassword, encodedPassword);
        }
        return MessageDigest.isEqual(
                rawPassword.toString().getBytes(StandardCharsets.UTF_8),
                encodedPassword.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.startsWith("{")) {
            return true;
        }
        return delegatingEncoder.upgradeEncoding(encodedPassword);
    }
}
