package com.devinolabs.uap.training.domain;

/**
 * Identifies which environment context was used for a feasibility analysis.
 */
public enum FeasibilityEnvironmentContextSource {

	EXPLICIT_ENVIRONMENT,
	DAY_OVERRIDE,
	PLAN_DEFAULT,
	ATHLETE_DEFAULT,
	OCCURRENCE_ACTUAL_SNAPSHOT,
	OCCURRENCE_PLANNED_SNAPSHOT,
	NONE

}
