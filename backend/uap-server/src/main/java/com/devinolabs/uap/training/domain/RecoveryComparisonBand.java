package com.devinolabs.uap.training.domain;

/**
 * Descriptive comparison of a target value against the athlete's own prior baseline.
 *
 * <p>Not a good/bad/dangerous health verdict.
 */
public enum RecoveryComparisonBand {

	FAR_BELOW_BASELINE,
	BELOW_BASELINE,
	WITHIN_BASELINE_RANGE,
	ABOVE_BASELINE,
	FAR_ABOVE_BASELINE,
	INSUFFICIENT_DATA

}
