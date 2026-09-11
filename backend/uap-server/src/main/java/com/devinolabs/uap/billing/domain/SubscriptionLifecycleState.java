package com.devinolabs.uap.billing.domain;

/**
 * Canonical internal subscription states (ADR-040). Not provider-native statuses.
 */
public enum SubscriptionLifecycleState {

	PENDING,
	TRIALING,
	ACTIVE,
	PAST_DUE,
	GRACE_PERIOD,
	CANCEL_AT_PERIOD_END,
	EXPIRED

}
