package com.fdmgroup.sprint4Deliverable.Model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "customer_claim_request")
public class CustomerClaimRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "claim_request_id")
	private Long claimRequestId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Customer customer;

	@Column(name = "claim_reference_hash", nullable = false, unique = true, length = 64)
	private String claimReferenceHash;

	@Column(name = "submitted_email", nullable = false, length = 320)
	private String submittedEmail;

	@Column(name = "submitted_phone_number", nullable = false, length = 16)
	private String submittedPhoneNumber;

	@Column(nullable = false, length = 16)
	private String state = "PENDING";

	@Column(nullable = false, updatable = false)
	private Instant submittedAt = Instant.now();

	private Instant reviewedAt;

	@Column(length = 260)
	private String reviewedBy;

	private Instant completedAt;

	protected CustomerClaimRequest() {
	}

	public CustomerClaimRequest(Customer customer, String claimReferenceHash,
			String submittedEmail, String submittedPhoneNumber) {
		this.customer = customer;
		this.claimReferenceHash = claimReferenceHash;
		this.submittedEmail = submittedEmail;
		this.submittedPhoneNumber = submittedPhoneNumber;
	}

	public Long getClaimRequestId() {
		return claimRequestId;
	}

	public Customer getCustomer() {
		return customer;
	}

	public String getClaimReferenceHash() {
		return claimReferenceHash;
	}

	public String getSubmittedEmail() {
		return submittedEmail;
	}

	public String getSubmittedPhoneNumber() {
		return submittedPhoneNumber;
	}

	public String getState() {
		return state;
	}

	public Instant getSubmittedAt() {
		return submittedAt;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public String getReviewedBy() {
		return reviewedBy;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public void approve(String reviewer) {
		transitionPending(reviewer, "APPROVED");
	}

	public void reject(String reviewer) {
		transitionPending(reviewer, "REJECTED");
	}

	public void complete() {
		if (!"APPROVED".equals(state)) {
			throw new IllegalStateException("Only approved claims can be completed.");
		}
		state = "COMPLETED";
		completedAt = Instant.now();
	}

	private void transitionPending(String reviewer, String nextState) {
		if (!"PENDING".equals(state)) {
			throw new IllegalStateException("Only pending claims can be reviewed.");
		}
		state = nextState;
		reviewedAt = Instant.now();
		reviewedBy = reviewer;
	}
}
