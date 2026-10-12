package com.devinolabs.uap.training.api.coachcontext;

/**
 * The account cannot be resolved to an athlete for a Coach read.
 * Missing optional facts are represented on the view, not by this exception.
 */
public class TrainingCoachContextUnavailableException extends RuntimeException {

	public TrainingCoachContextUnavailableException() {
		super("Training context is not available");
	}

}
