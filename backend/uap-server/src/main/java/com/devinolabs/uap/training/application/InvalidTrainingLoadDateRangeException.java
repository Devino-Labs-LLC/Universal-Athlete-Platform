package com.devinolabs.uap.training.application;

public class InvalidTrainingLoadDateRangeException extends RuntimeException {

	public InvalidTrainingLoadDateRangeException() {
		super("Invalid training load date range");
	}

	public InvalidTrainingLoadDateRangeException(Throwable cause) {
		super("Invalid training load date range", cause);
	}

}
