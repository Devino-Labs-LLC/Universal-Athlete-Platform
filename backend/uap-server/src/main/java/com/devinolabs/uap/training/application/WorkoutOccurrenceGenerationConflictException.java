package com.devinolabs.uap.training.application;

public class WorkoutOccurrenceGenerationConflictException extends RuntimeException {

	public WorkoutOccurrenceGenerationConflictException() {
		super("A concurrent generation already materialised this placement; retry the request");
	}

}
