package com.devinolabs.uap.training.domain;

/**
 * Scalar recovery metrics eligible for athlete-specific baseline and trend analytics.
 *
 * <p>Discomfort observations are excluded — they use a separate factual history aggregation.
 */
public enum RecoveryMetricType {

	SLEEP_DURATION,
	SLEEP_QUALITY,
	FATIGUE,
	MUSCLE_SORENESS,
	STRESS,
	MOOD,
	MOTIVATION;

	public RecoveryMetricDirection scaleDirection() {
		return switch (this) {
			case SLEEP_DURATION -> RecoveryMetricDirection.NEUTRAL_DIRECTION;
			case SLEEP_QUALITY, MOOD, MOTIVATION -> RecoveryMetricDirection.HIGHER_REPORTED_VALUE;
			case FATIGUE, MUSCLE_SORENESS, STRESS -> RecoveryMetricDirection.LOWER_REPORTED_VALUE;
		};
	}

	public boolean ordinalRating() {
		return this != SLEEP_DURATION;
	}

}
