package com.devinolabs.uap.athlete.application;

public class DuplicateAssessmentMeasurementException extends RuntimeException {

	public DuplicateAssessmentMeasurementException() {
		super("Measurement is already attached to this assessment");
	}

}
