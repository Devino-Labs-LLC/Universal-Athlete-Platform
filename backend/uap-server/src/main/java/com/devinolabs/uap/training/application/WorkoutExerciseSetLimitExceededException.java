package com.devinolabs.uap.training.application;

public class WorkoutExerciseSetLimitExceededException extends RuntimeException {

	public WorkoutExerciseSetLimitExceededException(int limit) {
		super("A workout exercise execution must not have more than " + limit + " sets");
	}

}
