package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalItemNotFoundException extends RuntimeException {

	public WorkoutAdaptationProposalItemNotFoundException() {
		this("Workout adaptation proposal item was not found");
	}

	public WorkoutAdaptationProposalItemNotFoundException(String message) {
		super(message);
	}

}
