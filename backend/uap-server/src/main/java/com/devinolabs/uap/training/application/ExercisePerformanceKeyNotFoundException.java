package com.devinolabs.uap.training.application;

public class ExercisePerformanceKeyNotFoundException extends RuntimeException {

	public ExercisePerformanceKeyNotFoundException() {
		super("Exercise performance key was not found for this athlete");
	}

}
