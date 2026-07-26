package com.devinolabs.uap.training.application;

public class TrainingPlanSchedulePlacementLockedException extends RuntimeException {

	public TrainingPlanSchedulePlacementLockedException() {
		super("Workout day placement cannot change while the schedule is ACTIVE "
				+ "and generated occurrences exist for this day");
	}

}
