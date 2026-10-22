package com.fdmgroup.sprint4Deliverable.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fdmgroup.sprint4Deliverable.Model.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
	
}
