package com.devinolabs.uap.athlete.application;

public class InvalidMeasurementTypeUnitCombinationException extends RuntimeException {

	public InvalidMeasurementTypeUnitCombinationException(String message) {
		super(message == null ? "Incompatible measurement type and unit" : message);
	}

}
