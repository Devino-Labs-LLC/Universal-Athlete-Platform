package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

import com.devinolabs.uap.billing.domain.ProviderCollectionState;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

/**
 * Applies the locked Slice F grace policy on top of a provider snapshot.
 * Reconciliation and generic snapshot sync never call {@link Subscription#establishGrace}.
 */
final class PaymentRecoveryApplier {

	static final Set<String> QUALIFYING_FAILURE_EVENTS = Set.of(
			"invoice.payment_failed",
			"invoice.payment_action_required");

	private PaymentRecoveryApplier() {
	}

	static boolean apply(
			Subscription subscription,
			String eventType,
			Instant eventCreatedAt,
			ProviderSubscriptionSnapshot snapshot,
			Clock clock) {
		SubscriptionLifecycleState before = subscription.lifecycleState();
		boolean changed = subscription.synchronizeProviderSnapshot(snapshot, clock);
		if (before == SubscriptionLifecycleState.GRACE_PERIOD && isQualifyingFailure(eventType, snapshot)) {
			changed = subscription.tightenGraceDeadline(eventCreatedAt, clock) || changed;
			return changed;
		}
		if (before != SubscriptionLifecycleState.ACTIVE && before != SubscriptionLifecycleState.TRIALING) {
			return changed;
		}
		if (!changed && !snapshot.providerStateAsOf().equals(subscription.providerStateAsOf())) {
			return false;
		}
		if (snapshot.collectionState() == ProviderCollectionState.PAST_DUE
				&& isQualifyingFailure(eventType, snapshot)
				&& (subscription.lifecycleState() == SubscriptionLifecycleState.ACTIVE
						|| subscription.lifecycleState() == SubscriptionLifecycleState.TRIALING)) {
			return subscription.establishGrace(snapshot, eventCreatedAt, clock);
		}
		if ((snapshot.collectionState() == ProviderCollectionState.UNPAID
				|| snapshot.collectionState() == ProviderCollectionState.PAUSED)
				&& (subscription.lifecycleState() == SubscriptionLifecycleState.ACTIVE
						|| subscription.lifecycleState() == SubscriptionLifecycleState.TRIALING)) {
			return subscription.recordExceptionalPaymentAttention(snapshot, clock) || changed;
		}
		return changed;
	}

	private static boolean isQualifyingFailure(String eventType, ProviderSubscriptionSnapshot snapshot) {
		if (QUALIFYING_FAILURE_EVENTS.contains(eventType)) {
			return true;
		}
		return "customer.subscription.updated".equals(eventType)
				&& snapshot.collectionState() == ProviderCollectionState.PAST_DUE;
	}

}
