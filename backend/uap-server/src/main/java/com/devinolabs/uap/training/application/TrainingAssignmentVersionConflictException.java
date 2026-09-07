package com.devinolabs.uap.training.application;

public class TrainingAssignmentVersionConflictException extends RuntimeException {

	public TrainingAssignmentVersionConflictException() {
		super("Training assignment was updated by another request");
	}

}
