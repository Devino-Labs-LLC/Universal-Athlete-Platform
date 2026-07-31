package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalEnvironmentRequiredException extends RuntimeException {

	public WorkoutAdaptationProposalEnvironmentRequiredException() {
		this("Workout occurrence environment context is required");
	}

	public WorkoutAdaptationProposalEnvironmentRequiredException(String message) {
		super(message);
	}

}
