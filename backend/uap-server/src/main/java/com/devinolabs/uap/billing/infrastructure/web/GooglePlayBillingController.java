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

import com.devinolabs.uap.billing.application.GooglePlayPurchaseValidationService;
import com.devinolabs.uap.billing.application.GooglePlayPurchaseValidationService.SubscriptionResult;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/billing/account/google-play")
@ConditionalOnProperty(prefix = "uap.billing.google-play", name = "enabled", havingValue = "true")
class GooglePlayBillingController {

	private final GooglePlayPurchaseValidationService purchaseValidationService;

	GooglePlayBillingController(GooglePlayPurchaseValidationService purchaseValidationService) {
		this.purchaseValidationService = purchaseValidationService;
	}

	@PostMapping("/purchases")
	@ResponseStatus(HttpStatus.OK)
	SubscriptionResult validateOrRestore(
			@Valid @RequestBody ValidateGooglePlayPurchaseRequest request,
			Authentication authentication) {
		return purchaseValidationService.validateOrRestore(
				accountId(authentication),
				request.purchaseToken(),
				request.productId());
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}

}
