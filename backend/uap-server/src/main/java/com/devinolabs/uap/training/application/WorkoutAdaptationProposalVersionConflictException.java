package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalVersionConflictException extends RuntimeException {

	public WorkoutAdaptationProposalVersionConflictException() {
		this("Workout adaptation proposal version conflict");
	}

	public WorkoutAdaptationProposalVersionConflictException(String message) {
		super(message);
	}

}
