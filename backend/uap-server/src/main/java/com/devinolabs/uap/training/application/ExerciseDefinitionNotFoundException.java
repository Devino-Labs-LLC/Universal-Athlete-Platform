package com.devinolabs.uap.training.application;

public class ExerciseDefinitionNotFoundException extends RuntimeException {

	public ExerciseDefinitionNotFoundException() {
		super("Exercise definition was not found");
	}

}
