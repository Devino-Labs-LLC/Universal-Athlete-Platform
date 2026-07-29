package com.devinolabs.uap.training.application;

public class DuplicateExerciseSubstitutionRelationshipException extends RuntimeException {

	public DuplicateExerciseSubstitutionRelationshipException() {
		super("An active substitution relationship with this source, target, and type already exists");
	}

}
