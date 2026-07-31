package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalNotAccessibleException extends RuntimeException {

	public WorkoutAdaptationProposalNotAccessibleException() {
		this("Workout adaptation proposal is not accessible");
	}

	public WorkoutAdaptationProposalNotAccessibleException(String message) {
		super(message);
	}

}
