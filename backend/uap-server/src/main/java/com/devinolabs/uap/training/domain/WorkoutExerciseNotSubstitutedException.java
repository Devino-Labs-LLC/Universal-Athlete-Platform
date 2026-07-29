package com.devinolabs.uap.training.domain;

public class WorkoutExerciseNotSubstitutedException extends RuntimeException {

	public WorkoutExerciseNotSubstitutedException() {
		this("The execution performs its prescribed exercise, so there is nothing to revert");
	}

	public WorkoutExerciseNotSubstitutedException(String message) {
		super(message);
	}

}
