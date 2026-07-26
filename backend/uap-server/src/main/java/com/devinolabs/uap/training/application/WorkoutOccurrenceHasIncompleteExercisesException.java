package com.devinolabs.uap.training.application;

public class WorkoutOccurrenceHasIncompleteExercisesException extends RuntimeException {

	public WorkoutOccurrenceHasIncompleteExercisesException() {
		super("All exercise executions must be completed or skipped before completing the occurrence");
	}

}
