package com.devinolabs.uap.athlete.application;

public class DuplicateAssessmentException extends RuntimeException {

	public DuplicateAssessmentException() {
		super("A matching non-cancelled assessment already exists");
	}

}
