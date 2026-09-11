package com.devinolabs.uap.billing.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Legal subscription lifecycle transitions (ADR-040). Terminal {@link SubscriptionLifecycleState#EXPIRED}
 * does not resurrect; a new commercial agreement uses a new subscription identity.
 */
final class SubscriptionLifecycleTransitions {

	private static final Map<SubscriptionLifecycleState, Set<SubscriptionLifecycleState>> ALLOWED =
			new EnumMap<>(SubscriptionLifecycleState.class);

	static {
		ALLOWED.put(SubscriptionLifecycleState.PENDING, EnumSet.of(
				SubscriptionLifecycleState.TRIALING,
				SubscriptionLifecycleState.ACTIVE,
				SubscriptionLifecycleState.EXPIRED));
		ALLOWED.put(SubscriptionLifecycleState.TRIALING, EnumSet.of(
				SubscriptionLifecycleState.ACTIVE,
				SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
				SubscriptionLifecycleState.EXPIRED));
		ALLOWED.put(SubscriptionLifecycleState.ACTIVE, EnumSet.of(
				SubscriptionLifecycleState.PAST_DUE,
				SubscriptionLifecycleState.GRACE_PERIOD,
				SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
				SubscriptionLifecycleState.EXPIRED));
		ALLOWED.put(SubscriptionLifecycleState.PAST_DUE, EnumSet.of(
				SubscriptionLifecycleState.GRACE_PERIOD,
				SubscriptionLifecycleState.ACTIVE,
				SubscriptionLifecycleState.EXPIRED));
		ALLOWED.put(SubscriptionLifecycleState.GRACE_PERIOD, EnumSet.of(
				SubscriptionLifecycleState.ACTIVE,
				SubscriptionLifecycleState.EXPIRED));
		ALLOWED.put(SubscriptionLifecycleState.CANCEL_AT_PERIOD_END, EnumSet.of(
				SubscriptionLifecycleState.ACTIVE,
				SubscriptionLifecycleState.EXPIRED));
		ALLOWED.put(SubscriptionLifecycleState.EXPIRED, EnumSet.noneOf(SubscriptionLifecycleState.class));
	}

	private SubscriptionLifecycleTransitions() {
	}

	static void requireAllowed(SubscriptionLifecycleState from, SubscriptionLifecycleState to) {
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(to, "to must not be null");
		if (from == to) {
			return;
		}
		Set<SubscriptionLifecycleState> next = ALLOWED.getOrDefault(from, Set.of());
		if (!next.contains(to)) {
			throw new IllegalSubscriptionTransitionException(from, to);
		}
	}

}
