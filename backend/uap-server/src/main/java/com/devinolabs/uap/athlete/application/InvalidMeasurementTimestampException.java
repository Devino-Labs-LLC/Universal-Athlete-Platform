package com.devinolabs.uap.athlete.application;

public class InvalidMeasurementTimestampException extends RuntimeException {

	public InvalidMeasurementTimestampException(String message) {
		super(message == null ? "Invalid measurement timestamp" : message);
	}

}
