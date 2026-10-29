package com.fdmgroup.sprint4Deliverable.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByTokenDigest(String tokenDigest);

    Optional<Session> findByAdminUserAndActiveTrue(AdminUser adminUser);
}
