package com.devinolabs.uap.training.application;

public class WorkoutSessionAlreadyExistsException extends RuntimeException {

	public WorkoutSessionAlreadyExistsException() {
		super("A workout session already exists for this workout exercise");
	}

}
