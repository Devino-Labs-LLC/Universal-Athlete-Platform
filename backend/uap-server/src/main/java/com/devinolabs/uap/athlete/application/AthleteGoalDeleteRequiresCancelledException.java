package com.devinolabs.uap.athlete.application;

public class AthleteGoalDeleteRequiresCancelledException extends RuntimeException {

	public AthleteGoalDeleteRequiresCancelledException() {
		super("Only cancelled goals may be deleted");
	}

}
