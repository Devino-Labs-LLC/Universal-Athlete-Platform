package com.devinolabs.uap.athlete.api;

public class AthleteGoalNotOwnedException extends RuntimeException {

	public AthleteGoalNotOwnedException() {
		super("Athlete goal was not found");
	}

}
