package com.devinolabs.uap.athlete.api;

/**
 * The authenticated account has no athlete profile the Coach is allowed to read.
 */
public class AthleteCoachProfileUnavailableException extends RuntimeException {

	public AthleteCoachProfileUnavailableException() {
		super("Athlete profile is not available");
	}

}
