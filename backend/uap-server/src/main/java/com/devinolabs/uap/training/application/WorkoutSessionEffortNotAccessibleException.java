package com.devinolabs.uap.training.application;

public class WorkoutSessionEffortNotAccessibleException extends RuntimeException {

	public WorkoutSessionEffortNotAccessibleException() {
		super("Session effort is not accessible");
	}

	public WorkoutSessionEffortNotAccessibleException(Throwable cause) {
		super("Session effort is not accessible", cause);
	}

}
