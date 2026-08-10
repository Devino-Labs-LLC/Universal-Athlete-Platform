package com.devinolabs.uap.training.application;

public class TrainingOverviewLoadFailedException extends RuntimeException {

	public TrainingOverviewLoadFailedException(String message) {
		super(message);
	}

	public TrainingOverviewLoadFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
