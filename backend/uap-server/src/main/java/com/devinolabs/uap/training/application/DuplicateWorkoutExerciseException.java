package com.devinolabs.uap.training.application;

public class DuplicateWorkoutExerciseException extends RuntimeException {

	public DuplicateWorkoutExerciseException() {
		super("A workout exercise with this name already exists for the workout day");
	}

}
