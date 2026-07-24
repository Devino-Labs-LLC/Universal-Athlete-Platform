package com.devinolabs.uap.training.application;

public class DuplicateWorkoutDayException extends RuntimeException {

	public DuplicateWorkoutDayException() {
		super("A workout day with the same title already exists in this training plan");
	}

}
