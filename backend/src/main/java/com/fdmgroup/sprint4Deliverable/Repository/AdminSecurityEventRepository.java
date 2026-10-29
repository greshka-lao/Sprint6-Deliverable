package com.fdmgroup.sprint4Deliverable.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fdmgroup.sprint4Deliverable.Model.AdminSecurityEvent;

public interface AdminSecurityEventRepository extends JpaRepository<AdminSecurityEvent, Long> {
}
