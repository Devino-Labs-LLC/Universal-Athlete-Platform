package com.devinolabs.uap.training.domain;

/**
 * Analytical result reason codes for recovery baselines and trends. Not HTTP errors.
 */
public enum RecoveryAnalyticsReasonCode {

	TARGET_VALUE_MISSING,
	NO_PRIOR_OBSERVATIONS,
	INSUFFICIENT_PRIOR_OBSERVATIONS,
	ZERO_BASELINE_VARIANCE,
	BASELINE_AVAILABLE,
	TREND_WINDOW_INSUFFICIENT,
	TRAINING_LOAD_CONTEXT_UNAVAILABLE

}
