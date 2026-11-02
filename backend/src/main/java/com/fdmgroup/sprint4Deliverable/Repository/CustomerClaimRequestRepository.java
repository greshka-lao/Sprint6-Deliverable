package com.fdmgroup.sprint4Deliverable.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fdmgroup.sprint4Deliverable.Model.CustomerClaimRequest;

public interface CustomerClaimRequestRepository extends JpaRepository<CustomerClaimRequest, Long> {
	Optional<CustomerClaimRequest> findByClaimReferenceHash(String claimReferenceHash);
	boolean existsByCustomer_CustomerIdAndState(long customerId, String state);
	List<CustomerClaimRequest> findByStateOrderBySubmittedAtAsc(String state);
}
