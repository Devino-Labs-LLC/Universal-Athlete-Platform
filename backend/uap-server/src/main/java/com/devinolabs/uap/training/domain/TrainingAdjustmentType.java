package com.devinolabs.uap.training.domain;

/**
 * Categorical adjustment concepts for TRAINING_RECOMMENDATION_V1.
 * No quantitative dosage is prescribed.
 */
public enum TrainingAdjustmentType {
	REDUCE_INTENSITY(1),
	REDUCE_TOTAL_VOLUME(2),
	REDUCE_SESSION_DURATION(3),
	INCREASE_REST(4),
	PREFER_LOWER_IMPACT_VARIATIONS(5),
	PREFER_EQUIPMENT_COMPATIBLE_VARIATIONS(6),
	OPTIONAL_RECOVERY_FOCUS(7),
	PRESERVE_PLANNED_SESSION(8),
	NO_ADJUSTMENT(9);

	private final int priority;

	TrainingAdjustmentType(int priority) {
		this.priority = priority;
	}

	public int priority() {
		return priority;
	}
}
