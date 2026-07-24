package com.devinolabs.uap.athlete.application;

public class InvalidAssessmentDateException extends RuntimeException {

	public InvalidAssessmentDateException(String message) {
		super(message == null ? "Invalid assessment date" : message);
	}

}
