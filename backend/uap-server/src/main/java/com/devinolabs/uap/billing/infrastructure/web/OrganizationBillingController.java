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

import com.devinolabs.uap.billing.application.OrganizationCheckoutService;
import com.devinolabs.uap.billing.application.OrganizationCheckoutService.CheckoutResult;
import com.devinolabs.uap.billing.application.OrganizationCheckoutService.SubscriptionResult;
import com.devinolabs.uap.billing.application.OrganizationSubscriptionManagementService;
import com.devinolabs.uap.billing.application.OrganizationSubscriptionManagementService.PortalSessionResult;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/billing/organizations/{organizationId}")
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
class OrganizationBillingController {

	private final OrganizationCheckoutService checkoutService;
	private final OrganizationSubscriptionManagementService managementService;

	OrganizationBillingController(
			OrganizationCheckoutService checkoutService,
			OrganizationSubscriptionManagementService managementService) {
		this.checkoutService = checkoutService;
		this.managementService = managementService;
	}

	@PostMapping("/checkout-sessions")
	@ResponseStatus(HttpStatus.CREATED)
	CheckoutResult createCheckout(
			@PathVariable UUID organizationId,
			@Valid @RequestBody CreateOrganizationCheckoutRequest request,
			Authentication authentication) {
		return checkoutService.startCheckout(
				accountId(authentication),
				organizationId,
				request.requestId(),
				request.planKey(),
				request.cadence());
	}

	@PostMapping("/subscriptions/{subscriptionId}/sync")
	SubscriptionResult synchronize(
			@PathVariable UUID organizationId,
			@PathVariable UUID subscriptionId,
			@Valid @RequestBody SynchronizeOrganizationSubscriptionRequest request,
			Authentication authentication) {
		return checkoutService.synchronize(
				accountId(authentication),
				organizationId,
				subscriptionId,
				request.checkoutSessionId());
	}

	@GetMapping
	SubscriptionResult current(
			@PathVariable UUID organizationId,
			Authentication authentication) {
		return checkoutService.currentStatus(accountId(authentication), organizationId);
	}

	@PostMapping("/portal-sessions")
	PortalSessionResult openPortal(
			@PathVariable UUID organizationId,
			Authentication authentication) {
		return managementService.openPortal(accountId(authentication), organizationId);
	}

	@PostMapping("/subscriptions/{subscriptionId}/plan-changes")
	SubscriptionResult changePlan(
			@PathVariable UUID organizationId,
			@PathVariable UUID subscriptionId,
			@Valid @RequestBody ChangeOrganizationPlanRequest request,
			Authentication authentication) {
		return managementService.changePlan(
				accountId(authentication),
				organizationId,
				subscriptionId,
				request.requestId(),
				request.targetPlanKey(),
				request.targetCadence());
	}

	@PostMapping("/subscriptions/{subscriptionId}/cancel")
	SubscriptionResult cancel(
			@PathVariable UUID organizationId,
			@PathVariable UUID subscriptionId,
			@Valid @RequestBody BillingMutationRequest request,
			Authentication authentication) {
		return managementService.cancel(
				accountId(authentication),
				organizationId,
				subscriptionId,
				request.requestId());
	}

	@PostMapping("/subscriptions/{subscriptionId}/reactivate")
	SubscriptionResult reactivate(
			@PathVariable UUID organizationId,
			@PathVariable UUID subscriptionId,
			@Valid @RequestBody BillingMutationRequest request,
			Authentication authentication) {
		return managementService.reactivate(
				accountId(authentication),
				organizationId,
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
