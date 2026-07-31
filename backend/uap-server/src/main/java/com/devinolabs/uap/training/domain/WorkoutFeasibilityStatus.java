package com.devinolabs.uap.training.domain;

/**
 * Overall workout feasibility for a day, plan, or occurrence analysis.
 *
 * <p>Feasibility reflects current prescribed or performed exercises only. Compatible substitution
 * candidates never promote a workout to {@link #FULLY_FEASIBLE}.
 */
public enum WorkoutFeasibilityStatus {

	FULLY_FEASIBLE,
	PARTIALLY_FEASIBLE,
	NOT_FEASIBLE,
	NO_ENVIRONMENT_CONTEXT,
	NO_EXERCISES

}
