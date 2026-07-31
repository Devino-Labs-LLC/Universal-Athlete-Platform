package com.devinolabs.uap.training.application;

public class InvalidFeasibilityEnvironmentModeException extends RuntimeException {

	public InvalidFeasibilityEnvironmentModeException() {
		super("Provide exactly one of trainingEnvironmentId or usePreferredEnvironments=true");
	}

}
