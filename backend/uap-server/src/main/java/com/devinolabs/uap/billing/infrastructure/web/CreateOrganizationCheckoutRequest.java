package com.devinolabs.uap.billing.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;

record CreateOrganizationCheckoutRequest(
		@NotNull UUID requestId,
		@NotNull CommercialPlanKey planKey,
		@NotNull BillingCadence cadence) {
}
