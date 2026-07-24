package com.devinolabs.uap.training.application;

public class TrainingPlanArchivedException extends RuntimeException {

	public TrainingPlanArchivedException() {
		super("Archived training plans cannot be modified");
	}

}
