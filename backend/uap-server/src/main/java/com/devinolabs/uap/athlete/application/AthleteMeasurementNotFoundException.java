package com.devinolabs.uap.athlete.application;

public class AthleteMeasurementNotFoundException extends RuntimeException {

	public AthleteMeasurementNotFoundException() {
		super("Athlete measurement was not found");
	}

}
