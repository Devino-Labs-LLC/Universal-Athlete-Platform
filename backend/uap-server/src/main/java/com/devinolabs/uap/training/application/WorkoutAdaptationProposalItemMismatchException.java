package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalItemMismatchException extends RuntimeException {

	public WorkoutAdaptationProposalItemMismatchException() {
		this("Workout adaptation proposal item does not belong to the proposal");
	}

	public WorkoutAdaptationProposalItemMismatchException(String message) {
		super(message);
	}

}
