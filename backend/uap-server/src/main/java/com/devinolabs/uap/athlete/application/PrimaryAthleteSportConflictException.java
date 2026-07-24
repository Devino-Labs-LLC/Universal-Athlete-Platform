package com.devinolabs.uap.athlete.application;

public class PrimaryAthleteSportConflictException extends RuntimeException {

	public PrimaryAthleteSportConflictException() {
		super("Athlete already has a primary sport");
	}

}
