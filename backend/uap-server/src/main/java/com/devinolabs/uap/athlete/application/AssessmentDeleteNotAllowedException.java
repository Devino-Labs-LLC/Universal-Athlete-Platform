package com.devinolabs.uap.athlete.application;

public class AssessmentDeleteNotAllowedException extends RuntimeException {

	public AssessmentDeleteNotAllowedException() {
		super("Only PLANNED or CANCELLED assessments may be deleted");
	}

}
