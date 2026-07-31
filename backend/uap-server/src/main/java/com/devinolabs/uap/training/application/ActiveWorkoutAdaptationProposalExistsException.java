package com.devinolabs.uap.training.application;

public class ActiveWorkoutAdaptationProposalExistsException extends RuntimeException {

	public ActiveWorkoutAdaptationProposalExistsException() {
		this("An active workout adaptation proposal already exists for this occurrence");
	}

	public ActiveWorkoutAdaptationProposalExistsException(String message) {
		super(message);
	}

}
