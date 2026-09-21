package com.devinolabs.uap.billing.application;

/**
 * Stripe-independent Organization owner usage snapshot. Null band fields mean no
 * fabricated numeric capacity (no effective band or ambiguous commercial state).
 */
public record OrganizationCapacitySnapshot(
		int activeAthleteCount,
		Integer bandCapacity,
		Integer remainingCapacity,
		boolean atCapacity,
		boolean overCapacity) {
}
