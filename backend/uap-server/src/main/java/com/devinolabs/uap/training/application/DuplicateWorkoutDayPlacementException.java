package com.devinolabs.uap.training.application;

public class DuplicateWorkoutDayPlacementException extends RuntimeException {

	public DuplicateWorkoutDayPlacementException() {
		super("Another workout day in this plan already occupies the same week, weekday and start time");
	}

}
