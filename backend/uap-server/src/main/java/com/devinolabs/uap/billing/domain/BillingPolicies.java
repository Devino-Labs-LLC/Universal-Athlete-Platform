package com.devinolabs.uap.billing.domain;

import java.time.Period;

/**
 * Locked commercial time policies for initial V4.
 */
public final class BillingPolicies {

	public static final Period ORGANIZATION_TRIAL_DURATION = Period.ofDays(14);

	public static final Period FAILED_PAYMENT_GRACE_DURATION = Period.ofDays(7);

	private BillingPolicies() {
	}

}
