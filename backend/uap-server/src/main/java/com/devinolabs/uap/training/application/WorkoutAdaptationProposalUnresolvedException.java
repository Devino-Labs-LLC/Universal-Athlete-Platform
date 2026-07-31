package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalUnresolvedException extends RuntimeException {

	public WorkoutAdaptationProposalUnresolvedException() {
		this("Workout adaptation proposal has unresolved items");
	}

	public WorkoutAdaptationProposalUnresolvedException(String message) {
		super(message);
	}

}
