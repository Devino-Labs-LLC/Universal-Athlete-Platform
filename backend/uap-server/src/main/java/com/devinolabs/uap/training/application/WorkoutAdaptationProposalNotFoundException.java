package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalNotFoundException extends RuntimeException {

	public WorkoutAdaptationProposalNotFoundException() {
		this("Workout adaptation proposal was not found");
	}

	public WorkoutAdaptationProposalNotFoundException(String message) {
		super(message);
	}

}
