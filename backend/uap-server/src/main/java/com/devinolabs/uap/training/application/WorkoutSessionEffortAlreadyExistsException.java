package com.devinolabs.uap.training.application;

public class WorkoutSessionEffortAlreadyExistsException extends RuntimeException {

	public WorkoutSessionEffortAlreadyExistsException() {
		super("Session effort already exists for this occurrence");
	}

	public WorkoutSessionEffortAlreadyExistsException(Throwable cause) {
		super("Session effort already exists for this occurrence", cause);
	}

}
