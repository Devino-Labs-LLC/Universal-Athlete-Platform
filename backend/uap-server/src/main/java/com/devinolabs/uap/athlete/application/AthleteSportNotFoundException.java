package com.devinolabs.uap.athlete.application;

public class AthleteSportNotFoundException extends RuntimeException {

	public AthleteSportNotFoundException() {
		super("Athlete sport was not found");
	}

}
