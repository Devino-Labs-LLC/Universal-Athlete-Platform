package com.devinolabs.uap.training.domain;

/**
 * Raised when an execution's prescribed identity, performed identity and substitution details do
 * not describe the same story, which would make the substitution log unreadable.
 */
public class WorkoutExerciseSubstitutionIdentityConflictException extends RuntimeException {

	public WorkoutExerciseSubstitutionIdentityConflictException(String message) {
		super(message);
	}

}
