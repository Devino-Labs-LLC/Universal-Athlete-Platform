package com.devinolabs.uap.training.application;

public class TrainingDashboardLoadFailedException extends RuntimeException {

	public TrainingDashboardLoadFailedException(String message) {
		super(message);
	}

	public TrainingDashboardLoadFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
