package com.devinolabs.uap.billing.application;

import java.util.UUID;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

final class BillingSnapshotAudit {

	private BillingSnapshotAudit() {
	}

	static void record(
			BillingAuditPort auditPort,
			UUID subscriptionId,
			UUID organizationId,
			UUID actorAccountId,
			CommercialPlanKey fromPlan,
			BillingCadence fromCadence,
			SubscriptionLifecycleState fromState,
			Subscription after) {
		if (fromPlan != after.planKey() || fromCadence != after.billingCadence()) {
			auditPort.planChanged(
					subscriptionId,
					organizationId,
					actorAccountId,
					fromPlan,
					after.planKey(),
					after.billingCadence());
		}
		if (after.lifecycleState() == SubscriptionLifecycleState.CANCEL_AT_PERIOD_END
				&& fromState != SubscriptionLifecycleState.CANCEL_AT_PERIOD_END) {
			auditPort.cancelRequested(subscriptionId, organizationId, actorAccountId);
		}
		if (fromState == SubscriptionLifecycleState.CANCEL_AT_PERIOD_END
				&& (after.lifecycleState() == SubscriptionLifecycleState.ACTIVE
						|| after.lifecycleState() == SubscriptionLifecycleState.TRIALING)) {
			auditPort.subscriptionReactivated(subscriptionId, organizationId, actorAccountId);
		}
		if (after.lifecycleState() == SubscriptionLifecycleState.EXPIRED
				&& fromState != SubscriptionLifecycleState.EXPIRED) {
			auditPort.subscriptionEnded(subscriptionId, organizationId);
		}
	}

}
