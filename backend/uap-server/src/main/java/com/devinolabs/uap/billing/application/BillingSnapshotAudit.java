package com.devinolabs.uap.billing.application;

import java.util.UUID;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingEndReason;
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
		if (after.lifecycleState() == SubscriptionLifecycleState.GRACE_PERIOD
				&& fromState != SubscriptionLifecycleState.GRACE_PERIOD) {
			auditPort.graceStarted(subscriptionId, organizationId);
		}
		if ((fromState == SubscriptionLifecycleState.GRACE_PERIOD
				|| fromState == SubscriptionLifecycleState.PAST_DUE)
				&& after.lifecycleState() == SubscriptionLifecycleState.ACTIVE) {
			auditPort.paymentRecovered(subscriptionId, organizationId);
		}
		else if (fromState == SubscriptionLifecycleState.PENDING
				&& (after.lifecycleState() == SubscriptionLifecycleState.ACTIVE
						|| after.lifecycleState() == SubscriptionLifecycleState.TRIALING)) {
			auditPort.subscriptionActivated(subscriptionId, organizationId, after.lifecycleState());
		}
		if (after.lifecycleState() == SubscriptionLifecycleState.EXPIRED
				&& fromState != SubscriptionLifecycleState.EXPIRED) {
			BillingEndReason reason = fromState == SubscriptionLifecycleState.GRACE_PERIOD
					|| fromState == SubscriptionLifecycleState.PAST_DUE
							? BillingEndReason.NONPAYMENT
							: BillingEndReason.UNSPECIFIED;
			auditPort.subscriptionEnded(subscriptionId, organizationId, reason);
		}
	}

	/**
	 * Account Individual Premium snapshot transitions (cancel / reactivate). Portal sessions
	 * are not audited — same policy as Organization §40.
	 */
	static void recordAccount(
			BillingAuditPort auditPort,
			UUID subscriptionId,
			UUID accountId,
			UUID actorAccountId,
			SubscriptionLifecycleState fromState,
			Subscription after) {
		if (after.lifecycleState() == SubscriptionLifecycleState.CANCEL_AT_PERIOD_END
				&& fromState != SubscriptionLifecycleState.CANCEL_AT_PERIOD_END) {
			auditPort.accountCancelRequested(subscriptionId, accountId, actorAccountId);
		}
		if (fromState == SubscriptionLifecycleState.CANCEL_AT_PERIOD_END
				&& (after.lifecycleState() == SubscriptionLifecycleState.ACTIVE
						|| after.lifecycleState() == SubscriptionLifecycleState.TRIALING)) {
			auditPort.accountSubscriptionReactivated(subscriptionId, accountId, actorAccountId);
		}
	}

}
