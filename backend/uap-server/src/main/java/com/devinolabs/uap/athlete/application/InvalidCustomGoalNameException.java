package com.devinolabs.uap.athlete.application;

public class InvalidCustomGoalNameException extends RuntimeException {

	public InvalidCustomGoalNameException(String message) {
		super(message == null ? "Invalid custom goal name" : message);
	}

}
