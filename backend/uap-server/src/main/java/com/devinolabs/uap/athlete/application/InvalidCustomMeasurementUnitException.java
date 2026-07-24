package com.devinolabs.uap.athlete.application;

public class InvalidCustomMeasurementUnitException extends RuntimeException {

	public InvalidCustomMeasurementUnitException(String message) {
		super(message == null ? "Invalid custom measurement unit" : message);
	}

}
