package com.devinolabs.uap.training.application;

public class WorkoutSessionNotFoundException extends RuntimeException {

	public WorkoutSessionNotFoundException() {
		super("Workout session was not found");
	}

}
