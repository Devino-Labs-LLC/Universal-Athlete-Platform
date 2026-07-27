package com.devinolabs.uap.training.domain;

/**
 * Raised when an athlete tries to rename or archive a shared SYSTEM exercise definition.
 */
public class SystemExerciseDefinitionModificationNotAllowedException extends RuntimeException {

	public SystemExerciseDefinitionModificationNotAllowedException() {
		super("System exercise definitions cannot be modified");
	}

	public SystemExerciseDefinitionModificationNotAllowedException(String message) {
		super(message);
	}

}
