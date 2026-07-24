package com.devinolabs.uap.athlete.application;

public class InvalidMeasurementDateRangeException extends RuntimeException {

	public InvalidMeasurementDateRangeException(String message) {
		super(message == null ? "Invalid measurement date range" : message);
	}

}
