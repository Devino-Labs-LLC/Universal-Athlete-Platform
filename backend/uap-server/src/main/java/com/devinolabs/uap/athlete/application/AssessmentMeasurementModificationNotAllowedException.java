package com.devinolabs.uap.athlete.application;

public class AssessmentMeasurementModificationNotAllowedException extends RuntimeException {

	public AssessmentMeasurementModificationNotAllowedException() {
		super("Assessment measurements can only be modified when status is PLANNED or IN_PROGRESS");
	}

}
