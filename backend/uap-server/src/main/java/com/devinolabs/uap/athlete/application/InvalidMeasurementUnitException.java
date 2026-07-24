package com.devinolabs.uap.athlete.application;

public class InvalidMeasurementUnitException extends RuntimeException {

	public InvalidMeasurementUnitException(String message) {
		super(message == null ? "Invalid measurement unit" : message);
	}

}
