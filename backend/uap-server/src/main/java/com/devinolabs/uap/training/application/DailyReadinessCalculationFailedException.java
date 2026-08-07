package com.devinolabs.uap.training.application;

public class DailyReadinessCalculationFailedException extends RuntimeException {

	public DailyReadinessCalculationFailedException(String message) {
		super(message);
	}

	public DailyReadinessCalculationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
