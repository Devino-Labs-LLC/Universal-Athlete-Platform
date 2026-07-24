package com.devinolabs.uap.training.application;

public class TrainingPlanDeleteNotAllowedException extends RuntimeException {

	public TrainingPlanDeleteNotAllowedException() {
		super("Only DRAFT training plans may be deleted");
	}

}
