package com.devinolabs.uap.training.application;

public class TrainingAssignmentNotFoundException extends RuntimeException {

	public TrainingAssignmentNotFoundException() {
		super("Training assignment was not found");
	}

}
