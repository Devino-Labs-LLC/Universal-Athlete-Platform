package com.devinolabs.uap.athlete.application;

public class InvalidAthleteGoalTargetException extends RuntimeException {

	public InvalidAthleteGoalTargetException(String message) {
		super(message == null ? "Invalid athlete goal target" : message);
	}

}
