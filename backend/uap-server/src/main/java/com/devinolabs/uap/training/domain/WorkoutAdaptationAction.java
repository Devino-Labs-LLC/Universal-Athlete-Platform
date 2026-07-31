package com.devinolabs.uap.training.domain;

/**
 * Adaptation action for one occurrence execution inside a proposal.
 *
 * <p>{@link #EXCLUDED} only excludes the item from bulk application; it does not skip the workout
 * execution itself.
 */
public enum WorkoutAdaptationAction {

	NO_CHANGE,
	SUBSTITUTE,
	UNRESOLVED,
	EXCLUDED

}
