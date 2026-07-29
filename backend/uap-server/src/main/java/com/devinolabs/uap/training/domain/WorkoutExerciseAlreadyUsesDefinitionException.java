package com.devinolabs.uap.training.domain;

public class WorkoutExerciseAlreadyUsesDefinitionException extends RuntimeException {

	public WorkoutExerciseAlreadyUsesDefinitionException() {
		this("The execution already performs this exercise definition");
	}

	public WorkoutExerciseAlreadyUsesDefinitionException(String message) {
		super(message);
	}

}
