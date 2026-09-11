package com.devinolabs.uap.billing.domain;

/**
 * Illegal subscription lifecycle transition (ADR-040).
 */
public class IllegalSubscriptionTransitionException extends IllegalStateException {

	public IllegalSubscriptionTransitionException(SubscriptionLifecycleState from, SubscriptionLifecycleState to) {
		super("Illegal subscription transition: " + from + " → " + to);
	}

}
