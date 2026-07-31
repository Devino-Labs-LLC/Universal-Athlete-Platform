package com.devinolabs.uap.training.domain;

/**
 * Current exercise feasibility status. Compatible suggestions are reported separately and never
 * imply that the current prescribed/performed exercise is feasible.
 */
public enum ExerciseFeasibilityStatus {

	FEASIBLE_AS_PRESCRIBED,
	FEASIBLE_AS_PERFORMED,
	MISSING_REQUIRED_EQUIPMENT,
	NO_COMPATIBLE_SUBSTITUTION,
	ARCHIVED_HISTORICAL_REFERENCE,
	NOT_ANALYZABLE

}
