package com.devinolabs.uap.athlete.application;

public class AssessmentCompletionRequiresMeasurementsException extends RuntimeException {

	public AssessmentCompletionRequiresMeasurementsException() {
		super("Assessment completion requires at least one attached measurement");
	}

}
