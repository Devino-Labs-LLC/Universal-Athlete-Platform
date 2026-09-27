package com.devinolabs.uap.billing.domain;

import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Locked commercial time policies for initial V4.
 */
public final class BillingPolicies {

	public static final Period ORGANIZATION_TRIAL_DURATION = Period.ofDays(14);

	public static final Period FAILED_PAYMENT_GRACE_DURATION = Period.ofDays(7);

	private BillingPolicies() {
	}

	/**
	 * Exclusive grace deadline: entitled while {@code asOf} is strictly before this instant.
	 * The anchor is a provider event timestamp, not local receipt time.
	 */
	public static Instant graceDeadline(Instant qualifyingFailureAt) {
		Objects.requireNonNull(qualifyingFailureAt, "qualifyingFailureAt must not be null");
		return qualifyingFailureAt.atZone(ZoneOffset.UTC).plus(FAILED_PAYMENT_GRACE_DURATION).toInstant();
	}

}
