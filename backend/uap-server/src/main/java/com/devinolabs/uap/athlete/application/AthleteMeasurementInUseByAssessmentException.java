package com.devinolabs.uap.athlete.application;

public class AthleteMeasurementInUseByAssessmentException extends RuntimeException {

	public AthleteMeasurementInUseByAssessmentException() {
		super("Measurement is attached to an active assessment and cannot be deleted");
	}

}
