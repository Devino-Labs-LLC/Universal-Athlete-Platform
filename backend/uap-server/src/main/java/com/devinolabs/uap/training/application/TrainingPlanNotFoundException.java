package com.devinolabs.uap.training.application;

public class TrainingPlanNotFoundException extends RuntimeException {

	public TrainingPlanNotFoundException() {
		super("Training plan was not found");
	}

}
