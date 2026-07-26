package com.devinolabs.uap.training.application;

public class WorkoutOccurrenceDeleteNotAllowedException extends RuntimeException {

	public WorkoutOccurrenceDeleteNotAllowedException() {
		this("Only SCHEDULED workout occurrences with untouched executions can be deleted");
	}

	public WorkoutOccurrenceDeleteNotAllowedException(String message) {
		super(message);
	}

}
