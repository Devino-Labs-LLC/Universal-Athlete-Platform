package com.devinolabs.uap.athlete.application;

public class DuplicateAthleteGoalException extends RuntimeException {

	public DuplicateAthleteGoalException() {
		super("An active or paused goal with the same type and title already exists");
	}

}
