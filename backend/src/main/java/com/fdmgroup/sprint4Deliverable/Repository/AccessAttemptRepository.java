package com.fdmgroup.sprint4Deliverable.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fdmgroup.sprint4Deliverable.Model.AccessAttempt;

public interface AccessAttemptRepository extends JpaRepository<AccessAttempt, Long> {

    List<AccessAttempt> findByUsernameOrderByCreatedAtDesc(String username);
}
