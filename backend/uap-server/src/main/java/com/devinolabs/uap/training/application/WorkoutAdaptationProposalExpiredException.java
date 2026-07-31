package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalExpiredException extends RuntimeException {

	public WorkoutAdaptationProposalExpiredException() {
		this("Workout adaptation proposal has expired");
	}

	public WorkoutAdaptationProposalExpiredException(String message) {
		super(message);
	}

}
