package com.devinolabs.uap.athlete.api;

public class AthleteNotFoundException extends RuntimeException {

	public AthleteNotFoundException() {
		super("Athlete profile was not found");
	}

}
