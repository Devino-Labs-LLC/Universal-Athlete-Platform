package com.devinolabs.uap.training.application;

public class DuplicateExerciseDefinitionException extends RuntimeException {

	public DuplicateExerciseDefinitionException() {
		super("An active exercise definition with this name already exists");
	}

}
