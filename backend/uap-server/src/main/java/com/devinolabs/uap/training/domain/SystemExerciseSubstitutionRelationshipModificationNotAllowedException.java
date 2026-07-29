package com.devinolabs.uap.training.domain;

public class SystemExerciseSubstitutionRelationshipModificationNotAllowedException extends RuntimeException {

	public SystemExerciseSubstitutionRelationshipModificationNotAllowedException() {
		super("SYSTEM exercise substitution relationships cannot be modified by athletes");
	}

}
