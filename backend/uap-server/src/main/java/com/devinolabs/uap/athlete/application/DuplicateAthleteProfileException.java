package com.devinolabs.uap.athlete.application;

public class DuplicateAthleteProfileException extends RuntimeException {

	public DuplicateAthleteProfileException() {
		super("An athlete profile already exists for this account");
	}

}
