package com.devinolabs.uap.training.domain;

import java.util.Optional;

public enum ReadinessDimensionType {
	FATIGUE,
	MUSCLE_SORENESS,
	STRESS,
	MOOD,
	MOTIVATION,
	SLEEP_QUALITY,
	SLEEP_DURATION,
	TRAINING_LOAD_CONTEXT;

	public boolean coreRecovery() {
		return this == FATIGUE
				|| this == MUSCLE_SORENESS
				|| this == STRESS
				|| this == MOOD
				|| this == MOTIVATION;
	}

	public boolean optionalRecovery() {
		return this == SLEEP_QUALITY || this == SLEEP_DURATION;
	}

	public Optional<RecoveryMetricType> sourceMetricType() {
		return switch (this) {
			case FATIGUE -> Optional.of(RecoveryMetricType.FATIGUE);
			case MUSCLE_SORENESS -> Optional.of(RecoveryMetricType.MUSCLE_SORENESS);
			case STRESS -> Optional.of(RecoveryMetricType.STRESS);
			case MOOD -> Optional.of(RecoveryMetricType.MOOD);
			case MOTIVATION -> Optional.of(RecoveryMetricType.MOTIVATION);
			case SLEEP_QUALITY -> Optional.of(RecoveryMetricType.SLEEP_QUALITY);
			case SLEEP_DURATION -> Optional.of(RecoveryMetricType.SLEEP_DURATION);
			case TRAINING_LOAD_CONTEXT -> Optional.empty();
		};
	}

}
