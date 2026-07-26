package com.devinolabs.uap.training.domain;

/**
 * How workout day placements repeat once the last plan week has been generated.
 * FINITE runs the plan weeks exactly once; REPEATING cycles back to week 1.
 */
public enum TrainingPlanRecurrenceMode {

	FINITE,
	REPEATING

}
