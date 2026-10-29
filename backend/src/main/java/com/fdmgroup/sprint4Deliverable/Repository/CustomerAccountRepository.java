package com.fdmgroup.sprint4Deliverable.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fdmgroup.sprint4Deliverable.Model.CustomerAccount;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
	Optional<CustomerAccount> findByUsername(String username);
	Optional<CustomerAccount> findByEmail(String email);
	boolean existsByEmail(String email);
	boolean existsByUsername(String username);
	List<CustomerAccount> findByUsernameStartingWith(String prefix);
}
