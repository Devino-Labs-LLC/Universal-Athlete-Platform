package com.devinolabs.uap.training.application;

public class WorkoutOccurrenceRequiresExercisesException extends RuntimeException {

	public WorkoutOccurrenceRequiresExercisesException() {
		super("Workout day must have at least one exercise to schedule an occurrence");
	}

}
