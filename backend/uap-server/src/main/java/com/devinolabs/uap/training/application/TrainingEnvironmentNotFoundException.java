package com.devinolabs.uap.training.application;

public class TrainingEnvironmentNotFoundException extends RuntimeException {
	public TrainingEnvironmentNotFoundException() { super(); }
	public TrainingEnvironmentNotFoundException(String message) { super(message); }
}
