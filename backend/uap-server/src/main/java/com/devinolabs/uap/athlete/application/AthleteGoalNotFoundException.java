package com.devinolabs.uap.athlete.application;

public class AthleteGoalNotFoundException extends RuntimeException {

	public AthleteGoalNotFoundException() {
		super("Athlete goal was not found");
	}

}
