package com.devinolabs.uap.athlete.application;

public class AssessmentNotFoundException extends RuntimeException {

	public AssessmentNotFoundException() {
		super("Assessment was not found");
	}

}
