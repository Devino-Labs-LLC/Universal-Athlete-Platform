package com.devinolabs.uap.training.application;

public class WorkoutExerciseExecutionHasIncompleteSetsException extends RuntimeException {

	public WorkoutExerciseExecutionHasIncompleteSetsException() {
		super("All sets must be completed or skipped before completing the exercise execution");
	}

}
