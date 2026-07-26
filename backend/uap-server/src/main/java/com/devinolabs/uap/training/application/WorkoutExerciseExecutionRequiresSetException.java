package com.devinolabs.uap.training.application;

public class WorkoutExerciseExecutionRequiresSetException extends RuntimeException {

	public WorkoutExerciseExecutionRequiresSetException() {
		super("A workout exercise execution must keep at least one set");
	}

}
