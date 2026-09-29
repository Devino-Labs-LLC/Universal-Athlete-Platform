package com.devinolabs.uap.billing.infrastructure.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.billing.application.ApplePurchaseValidationService;
import com.devinolabs.uap.billing.application.ApplePurchaseValidationService.SubscriptionResult;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/billing/account/apple")
@ConditionalOnProperty(prefix = "uap.billing.apple", name = "enabled", havingValue = "true")
class AppleBillingController {

	private final ApplePurchaseValidationService purchaseValidationService;

	AppleBillingController(ApplePurchaseValidationService purchaseValidationService) {
		this.purchaseValidationService = purchaseValidationService;
	}

	@PostMapping("/transactions")
	@ResponseStatus(HttpStatus.OK)
	SubscriptionResult validateOrRestore(
			@Valid @RequestBody ValidateAppleTransactionRequest request,
			Authentication authentication) {
		return purchaseValidationService.validateOrRestore(
				accountId(authentication),
				request.signedTransactionInfo());
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}

}
