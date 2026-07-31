package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalStaleException extends RuntimeException {

	public WorkoutAdaptationProposalStaleException() {
		this("Workout adaptation proposal is stale and must be regenerated");
	}

	public WorkoutAdaptationProposalStaleException(String message) {
		super(message);
	}

}
