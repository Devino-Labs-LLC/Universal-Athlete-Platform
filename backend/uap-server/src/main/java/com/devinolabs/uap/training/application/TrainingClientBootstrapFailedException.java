package com.devinolabs.uap.training.application;

public class TrainingClientBootstrapFailedException extends RuntimeException {

	public TrainingClientBootstrapFailedException(String message) {
		super(message);
	}

	public TrainingClientBootstrapFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
