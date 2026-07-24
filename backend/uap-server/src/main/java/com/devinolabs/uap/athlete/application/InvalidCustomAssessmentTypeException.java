package com.devinolabs.uap.athlete.application;

public class InvalidCustomAssessmentTypeException extends RuntimeException {

	public InvalidCustomAssessmentTypeException(String message) {
		super(message == null ? "Invalid custom assessment type" : message);
	}

}
