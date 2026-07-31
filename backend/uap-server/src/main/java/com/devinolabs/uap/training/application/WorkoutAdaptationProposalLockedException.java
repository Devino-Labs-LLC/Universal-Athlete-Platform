package com.devinolabs.uap.training.application;

public class WorkoutAdaptationProposalLockedException extends RuntimeException {

	public WorkoutAdaptationProposalLockedException() {
		this("Workout adaptation proposal cannot be applied because substitution is locked");
	}

	public WorkoutAdaptationProposalLockedException(String message) {
		super(message);
	}

}
