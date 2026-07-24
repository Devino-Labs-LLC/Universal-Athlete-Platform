package com.devinolabs.uap.athlete.application;

public class InvalidGoalTargetDateException extends RuntimeException {

	public InvalidGoalTargetDateException(String message) {
		super(message == null ? "Invalid goal target date" : message);
	}

}
