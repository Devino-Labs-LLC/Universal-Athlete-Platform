package com.devinolabs.uap.training.application;

public class WorkoutDayDeleteNotAllowedException extends RuntimeException {

	public WorkoutDayDeleteNotAllowedException() {
		super("Only PLANNED workout days may be deleted");
	}

}
