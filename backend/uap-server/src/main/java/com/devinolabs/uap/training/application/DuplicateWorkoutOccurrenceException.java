package com.devinolabs.uap.training.application;

public class DuplicateWorkoutOccurrenceException extends RuntimeException {

	public DuplicateWorkoutOccurrenceException() {
		super("An active workout occurrence already exists for this day and date");
	}

}
