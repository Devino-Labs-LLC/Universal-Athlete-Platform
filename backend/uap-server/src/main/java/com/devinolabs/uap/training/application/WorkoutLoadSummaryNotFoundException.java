package com.devinolabs.uap.training.application;

public class WorkoutLoadSummaryNotFoundException extends RuntimeException {

	public WorkoutLoadSummaryNotFoundException() {
		super("Occurrence load summary was not found");
	}

	public WorkoutLoadSummaryNotFoundException(Throwable cause) {
		super("Occurrence load summary was not found", cause);
	}

}
