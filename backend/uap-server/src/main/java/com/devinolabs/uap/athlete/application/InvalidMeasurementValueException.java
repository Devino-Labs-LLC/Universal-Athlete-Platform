package com.devinolabs.uap.athlete.application;

public class InvalidMeasurementValueException extends RuntimeException {

	public InvalidMeasurementValueException(String message) {
		super(message == null ? "Invalid measurement value" : message);
	}

}
