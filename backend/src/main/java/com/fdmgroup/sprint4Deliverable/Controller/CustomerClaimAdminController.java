package com.fdmgroup.sprint4Deliverable.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fdmgroup.sprint4Deliverable.Model.AdminCustomerClaimItem;
import com.fdmgroup.sprint4Deliverable.Model.AdminUser;
import com.fdmgroup.sprint4Deliverable.Service.AuthService;
import com.fdmgroup.sprint4Deliverable.Service.CustomerClaimService;
import com.fdmgroup.sprint4Deliverable.Service.CustomerClaimService.ClaimRequestNotFoundException;

@RestController
@RequestMapping("/customer/claims")
public class CustomerClaimAdminController {

	private final AuthService authService;
	private final CustomerClaimService claimService;

	public CustomerClaimAdminController(AuthService authService, CustomerClaimService claimService) {
		this.authService = authService;
		this.claimService = claimService;
	}

	@GetMapping
	public List<AdminCustomerClaimItem> pendingClaims(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
		authService.validateSession(extractToken(authorizationHeader));
		return claimService.pendingClaims();
	}

	@PostMapping("/{claimRequestId}/approve")
	public ResponseEntity<?> approve(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long claimRequestId) {
		AdminUser reviewer = authService.validateSession(extractToken(authorizationHeader));
		return review(claimRequestId, reviewer, true);
	}

	@PostMapping("/{claimRequestId}/reject")
	public ResponseEntity<?> reject(
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@PathVariable long claimRequestId) {
		AdminUser reviewer = authService.validateSession(extractToken(authorizationHeader));
		return review(claimRequestId, reviewer, false);
	}

	private ResponseEntity<?> review(long claimRequestId, AdminUser reviewer, boolean approve) {
		try {
			if (approve) {
				claimService.approve(claimRequestId, reviewer.getUsername());
			} else {
				claimService.reject(claimRequestId, reviewer.getUsername());
			}
			return ResponseEntity.noContent().build();
		} catch (ClaimRequestNotFoundException exception) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
					"code", "claim_not_found", "message", "The claim request was not found."));
		} catch (IllegalStateException exception) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
					"code", "claim_already_reviewed", "message", "Only pending claims can be reviewed."));
		}
	}

	private String extractToken(String authorizationHeader) {
		if (authorizationHeader == null || authorizationHeader.isBlank()) {
			return null;
		}
		String[] parts = authorizationHeader.split(" ");
		return parts.length == 2 && "Bearer".equalsIgnoreCase(parts[0]) ? parts[1] : authorizationHeader;
	}
}
