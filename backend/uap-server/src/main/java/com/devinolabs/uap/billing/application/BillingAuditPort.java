package com.devinolabs.uap.billing.application;

import java.util.UUID;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

public interface BillingAuditPort {

	void checkoutInitiated(
			UUID subscriptionId,
			UUID organizationId,
			UUID actorAccountId,
			CommercialPlanKey planKey,
			BillingCadence cadence);

	void subscriptionSynchronized(
			UUID subscriptionId,
			UUID organizationId,
			UUID actorAccountId,
			SubscriptionLifecycleState lifecycleState);

	void subscriptionActivated(
			UUID subscriptionId,
			UUID organizationId,
			SubscriptionLifecycleState lifecycleState);

}
