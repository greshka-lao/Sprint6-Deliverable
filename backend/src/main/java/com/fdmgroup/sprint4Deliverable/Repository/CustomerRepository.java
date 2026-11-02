package com.fdmgroup.sprint4Deliverable.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fdmgroup.sprint4Deliverable.Model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
	Optional<Customer> findByUsername(String username);
	Optional<Customer> findByEmail(String email);
	List<Customer> findByNameAndCustomerType(String name, String customerType);
	boolean existsByEmail(String email);
	boolean existsByUsername(String username);
	List<Customer> findByUsernameStartingWith(String prefix);
	List<Customer> findByStatus(String status);
}
