package com.devinolabs.uap.training.application;

public class WorkoutLoadCalculationFailedException extends RuntimeException {

	public WorkoutLoadCalculationFailedException() {
		super("Occurrence load calculation failed");
	}

	public WorkoutLoadCalculationFailedException(Throwable cause) {
		super("Occurrence load calculation failed", cause);
	}

}
