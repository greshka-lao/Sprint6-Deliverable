package com.fdmgroup.sprint4Deliverable.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Model.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByToken(String token);

    Optional<Session> findByAdminUserAndActiveTrue(AdminUser adminUser);
}
