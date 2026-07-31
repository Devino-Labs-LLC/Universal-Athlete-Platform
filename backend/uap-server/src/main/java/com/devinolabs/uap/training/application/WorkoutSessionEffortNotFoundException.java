package com.devinolabs.uap.training.application;

public class WorkoutSessionEffortNotFoundException extends RuntimeException {

	public WorkoutSessionEffortNotFoundException() {
		super("Session effort was not found");
	}

	public WorkoutSessionEffortNotFoundException(Throwable cause) {
		super("Session effort was not found", cause);
	}

}
