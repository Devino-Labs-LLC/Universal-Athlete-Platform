package com.devinolabs.uap.athlete.application;

public class InvalidAssessmentStatusException extends RuntimeException {

	public InvalidAssessmentStatusException(String message) {
		super(message == null ? "Invalid assessment status transition" : message);
	}

}
