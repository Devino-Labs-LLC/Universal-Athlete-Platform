package com.devinolabs.uap.athlete.application;

public class InvalidCustomMeasurementNameException extends RuntimeException {

	public InvalidCustomMeasurementNameException(String message) {
		super(message == null ? "Invalid custom measurement name" : message);
	}

}
