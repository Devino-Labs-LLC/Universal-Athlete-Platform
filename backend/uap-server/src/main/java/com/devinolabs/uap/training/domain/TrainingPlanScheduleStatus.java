package com.devinolabs.uap.training.domain;

/**
 * Calendar activation state of a training plan.
 * Independent from {@link TrainingPlanStatus}, which tracks plan ownership and content lifecycle.
 */
public enum TrainingPlanScheduleStatus {

	DRAFT,
	ACTIVE,
	PAUSED,
	COMPLETED

}
