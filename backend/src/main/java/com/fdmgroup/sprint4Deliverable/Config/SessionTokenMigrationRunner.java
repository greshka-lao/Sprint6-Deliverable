package com.fdmgroup.sprint4Deliverable.Config;

import java.util.regex.Pattern;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fdmgroup.sprint4Deliverable.Model.Session;
import com.fdmgroup.sprint4Deliverable.Repository.SessionRepository;
import com.fdmgroup.sprint4Deliverable.Security.TokenDigest;

@Component
@Order(1)
public class SessionTokenMigrationRunner implements ApplicationRunner {

    private static final Pattern SHA_256_HEX = Pattern.compile("^[0-9a-f]{64}$");

    private final SessionRepository sessionRepository;

    public SessionTokenMigrationRunner(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Session session : sessionRepository.findAll()) {
            String storedValue = session.getTokenDigest();
            if (storedValue != null && !SHA_256_HEX.matcher(storedValue).matches()) {
                session.setTokenDigest(TokenDigest.sha256(storedValue));
                sessionRepository.save(session);
            }
        }
    }
}
