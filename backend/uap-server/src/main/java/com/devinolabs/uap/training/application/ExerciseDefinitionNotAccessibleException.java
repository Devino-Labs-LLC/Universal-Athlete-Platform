package com.devinolabs.uap.training.application;

/**
 * Raised when a definition exists but belongs to another athlete.
 *
 * <p>Reported as 404 rather than 403 so that one athlete cannot probe for the existence of
 * another's custom exercises.
 */
public class ExerciseDefinitionNotAccessibleException extends RuntimeException {

	public ExerciseDefinitionNotAccessibleException() {
		super("Exercise definition is not accessible");
	}

}
