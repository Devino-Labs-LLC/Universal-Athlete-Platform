package com.devinolabs.uap.training.application;

public class WorkoutSessionEffortNotAllowedException extends RuntimeException {

	public WorkoutSessionEffortNotAllowedException() {
		super("Session effort is not allowed for this occurrence");
	}

	public WorkoutSessionEffortNotAllowedException(Throwable cause) {
		super("Session effort is not allowed for this occurrence", cause);
	}

}
