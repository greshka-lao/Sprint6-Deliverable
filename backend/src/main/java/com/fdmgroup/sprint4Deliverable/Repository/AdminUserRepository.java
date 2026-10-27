package com.fdmgroup.sprint4Deliverable.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fdmgroup.sprint4Deliverable.Model.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsername(String username);

    Optional<AdminUser> findByEmail(String email);

    @Query("SELECT a FROM AdminUser a LEFT JOIN FETCH a.role WHERE a.username = :username OR a.email = :email")
    Optional<AdminUser> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    boolean existsByUsername(String username);
}
