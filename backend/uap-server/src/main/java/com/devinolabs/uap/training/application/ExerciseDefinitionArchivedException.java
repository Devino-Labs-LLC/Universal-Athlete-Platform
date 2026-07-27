package com.devinolabs.uap.training.application;

public class ExerciseDefinitionArchivedException extends RuntimeException {

	public ExerciseDefinitionArchivedException() {
		super("Archived exercise definitions cannot be prescribed");
	}

	public ExerciseDefinitionArchivedException(String message) {
		super(message);
	}

}
