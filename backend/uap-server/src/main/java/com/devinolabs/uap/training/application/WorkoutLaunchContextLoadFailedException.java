package com.devinolabs.uap.training.application;

public class WorkoutLaunchContextLoadFailedException extends RuntimeException {

	public WorkoutLaunchContextLoadFailedException(String message) {
		super(message);
	}

	public WorkoutLaunchContextLoadFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
