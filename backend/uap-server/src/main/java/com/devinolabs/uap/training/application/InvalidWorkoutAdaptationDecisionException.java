package com.devinolabs.uap.training.application;

public class InvalidWorkoutAdaptationDecisionException extends RuntimeException {

	public InvalidWorkoutAdaptationDecisionException() {
		this("Invalid workout adaptation decision");
	}

	public InvalidWorkoutAdaptationDecisionException(String message) {
		super(message);
	}

}
