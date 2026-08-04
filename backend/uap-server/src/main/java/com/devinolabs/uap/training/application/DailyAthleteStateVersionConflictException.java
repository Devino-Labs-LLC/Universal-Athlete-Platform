package com.devinolabs.uap.training.application;

public class DailyAthleteStateVersionConflictException extends RuntimeException {

	public DailyAthleteStateVersionConflictException(String message) {
		super(message);
	}

	public DailyAthleteStateVersionConflictException(String message, Throwable cause) {
		super(message, cause);
	}

}
