package com.devinolabs.uap.training.domain;

/**
 * The normal performance shape of an exercise. Guides UI and validation hints without invalidating
 * historical sets that recorded additional legitimate metrics.
 */
public enum ExerciseMetricMode {

	REPETITIONS,
	WEIGHT_AND_REPETITIONS,
	DURATION,
	DISTANCE,
	DISTANCE_AND_DURATION,
	REPETITIONS_AND_DURATION,
	WEIGHT_AND_DURATION,
	MIXED

}
