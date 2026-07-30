package com.devinolabs.uap.training.application;

public class WorkoutOccurrenceEnvironmentLockedException extends RuntimeException {

	public WorkoutOccurrenceEnvironmentLockedException() {
		super("Workout occurrence environment cannot be changed after workout activity has begun");
	}

}
