package com.devinolabs.uap.billing.infrastructure.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.billing.application.IndividualCheckoutService;
import com.devinolabs.uap.billing.application.IndividualCheckoutService.CheckoutResult;
import com.devinolabs.uap.billing.application.IndividualCheckoutService.SubscriptionResult;
import com.devinolabs.uap.billing.application.IndividualSubscriptionManagementService;
import com.devinolabs.uap.billing.application.IndividualSubscriptionManagementService.PortalSessionResult;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/billing/account")
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
class IndividualBillingController {

	private final IndividualCheckoutService checkoutService;
	private final IndividualSubscriptionManagementService managementService;

	IndividualBillingController(
			IndividualCheckoutService checkoutService,
			IndividualSubscriptionManagementService managementService) {
		this.checkoutService = checkoutService;
		this.managementService = managementService;
	}

	@PostMapping("/checkout-sessions")
	@ResponseStatus(HttpStatus.CREATED)
	CheckoutResult createCheckout(
			@Valid @RequestBody CreateIndividualCheckoutRequest request,
			Authentication authentication) {
		return checkoutService.startCheckout(
				accountId(authentication),
				request.requestId(),
				request.planKey(),
				request.cadence());
	}

	@PostMapping("/subscriptions/{subscriptionId}/sync")
	SubscriptionResult synchronize(
			@PathVariable UUID subscriptionId,
			@Valid @RequestBody SynchronizeIndividualSubscriptionRequest request,
			Authentication authentication) {
		return checkoutService.synchronize(
				accountId(authentication),
				subscriptionId,
				request.checkoutSessionId());
	}

	@GetMapping
	SubscriptionResult current(Authentication authentication) {
		return checkoutService.currentStatus(accountId(authentication));
	}

	@PostMapping("/portal-sessions")
	PortalSessionResult openPortal(Authentication authentication) {
		return managementService.openPortal(accountId(authentication));
	}

	@PostMapping("/subscriptions/{subscriptionId}/cancel")
	SubscriptionResult cancel(
			@PathVariable UUID subscriptionId,
			@Valid @RequestBody BillingMutationRequest request,
			Authentication authentication) {
		return managementService.cancel(
				accountId(authentication),
				subscriptionId,
				request.requestId());
	}

	@PostMapping("/subscriptions/{subscriptionId}/reactivate")
	SubscriptionResult reactivate(
			@PathVariable UUID subscriptionId,
			@Valid @RequestBody BillingMutationRequest request,
			Authentication authentication) {
		return managementService.reactivate(
				accountId(authentication),
				subscriptionId,
				request.requestId());
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}

}
