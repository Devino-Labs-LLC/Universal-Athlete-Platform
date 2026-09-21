package com.devinolabs.uap.billing.infrastructure.web;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.billing.application.GetOrganizationCapacityUseCase;
import com.devinolabs.uap.billing.application.OrganizationCapacitySnapshot;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/billing/organizations/{organizationId}/capacity")
class OrganizationCapacityController {

	private final GetOrganizationCapacityUseCase getOrganizationCapacityUseCase;

	OrganizationCapacityController(GetOrganizationCapacityUseCase getOrganizationCapacityUseCase) {
		this.getOrganizationCapacityUseCase = getOrganizationCapacityUseCase;
	}

	@GetMapping
	OrganizationCapacitySnapshot get(@PathVariable UUID organizationId, Authentication authentication) {
		return getOrganizationCapacityUseCase.execute(accountId(authentication), organizationId);
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}

}
