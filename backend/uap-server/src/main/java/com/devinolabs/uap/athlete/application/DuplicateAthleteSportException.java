package com.devinolabs.uap.athlete.application;

public class DuplicateAthleteSportException extends RuntimeException {

	public DuplicateAthleteSportException() {
		super("Athlete already participates in this sport");
	}

}
