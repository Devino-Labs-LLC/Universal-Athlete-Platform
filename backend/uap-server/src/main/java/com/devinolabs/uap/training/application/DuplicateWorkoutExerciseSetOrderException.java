package com.devinolabs.uap.training.application;

public class DuplicateWorkoutExerciseSetOrderException extends RuntimeException {

	public DuplicateWorkoutExerciseSetOrderException() {
		super("A workout exercise set with the same number or display order already exists");
	}

}
