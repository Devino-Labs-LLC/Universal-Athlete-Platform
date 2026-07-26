package com.devinolabs.uap.training.application;

public class DuplicateWorkoutExerciseExecutionException extends RuntimeException {

	public DuplicateWorkoutExerciseExecutionException() {
		super("A workout exercise execution already exists for this occurrence");
	}

}
