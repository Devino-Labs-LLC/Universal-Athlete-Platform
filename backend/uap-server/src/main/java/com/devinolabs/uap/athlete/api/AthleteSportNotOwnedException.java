package com.devinolabs.uap.athlete.api;

public class AthleteSportNotOwnedException extends RuntimeException {

	public AthleteSportNotOwnedException() {
		super("Athlete sport was not found");
	}

}
