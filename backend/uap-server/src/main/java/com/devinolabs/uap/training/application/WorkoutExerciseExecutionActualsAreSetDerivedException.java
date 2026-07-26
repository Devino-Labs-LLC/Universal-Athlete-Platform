package com.devinolabs.uap.training.application;

public class WorkoutExerciseExecutionActualsAreSetDerivedException extends RuntimeException {

	public WorkoutExerciseExecutionActualsAreSetDerivedException() {
		super("Execution actuals are derived from set-level logging; log the individual sets instead");
	}

}
