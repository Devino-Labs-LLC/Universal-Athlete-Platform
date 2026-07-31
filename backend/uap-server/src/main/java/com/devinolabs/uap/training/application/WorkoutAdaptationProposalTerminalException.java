package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalTerminalException extends RuntimeException {

	public WorkoutAdaptationProposalTerminalException() {
		this("Workout adaptation proposal is in a terminal state");
	}

	public WorkoutAdaptationProposalTerminalException(String message) {
		super(message);
	}

}
