package com.devinolabs.uap.athlete.application;

public class InvalidAthleteGoalStatusTransitionException extends RuntimeException {

	public InvalidAthleteGoalStatusTransitionException(String message) {
		super(message == null ? "Invalid athlete goal status transition" : message);
	}

}
