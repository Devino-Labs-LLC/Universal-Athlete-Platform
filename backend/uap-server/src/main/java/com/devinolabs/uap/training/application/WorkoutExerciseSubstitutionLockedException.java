package com.devinolabs.uap.training.application;

public class WorkoutExerciseSubstitutionLockedException extends RuntimeException {

	public WorkoutExerciseSubstitutionLockedException() {
		this("The exercise can no longer be substituted once any of its sets has been started,"
				+ " completed or skipped");
	}

	public WorkoutExerciseSubstitutionLockedException(String message) {
		super(message);
	}

}
