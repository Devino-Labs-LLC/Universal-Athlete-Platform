package com.devinolabs.uap.training.application;

public class WorkoutDayNotFoundException extends RuntimeException {

	public WorkoutDayNotFoundException() {
		super("Workout day was not found");
	}

}
