package com.devinolabs.uap.training.application;

public class InvalidTrainingLoadGranularityException extends RuntimeException {

	public InvalidTrainingLoadGranularityException() {
		super("Invalid training load granularity");
	}

	public InvalidTrainingLoadGranularityException(Throwable cause) {
		super("Invalid training load granularity", cause);
	}

}
