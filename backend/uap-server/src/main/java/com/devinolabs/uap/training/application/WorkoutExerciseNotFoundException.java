package com.devinolabs.uap.training.application;

public class WorkoutExerciseNotFoundException extends RuntimeException {

	public WorkoutExerciseNotFoundException() {
		super("Workout exercise was not found");
	}

}
