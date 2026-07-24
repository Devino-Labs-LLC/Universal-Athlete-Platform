package com.devinolabs.uap.training.application;

public class DuplicateTrainingPlanException extends RuntimeException {

	public DuplicateTrainingPlanException() {
		super("A non-archived training plan with the same name and overlapping dates already exists");
	}

}
