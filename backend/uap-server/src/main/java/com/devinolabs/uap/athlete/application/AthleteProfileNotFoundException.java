package com.devinolabs.uap.athlete.application;

public class AthleteProfileNotFoundException extends RuntimeException {

	public AthleteProfileNotFoundException() {
		super("Athlete profile was not found");
	}

}
