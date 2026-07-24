package com.devinolabs.uap.athlete.application;

public class AssessmentMeasurementNotFoundException extends RuntimeException {

	public AssessmentMeasurementNotFoundException() {
		super("Assessment measurement was not found");
	}

}
