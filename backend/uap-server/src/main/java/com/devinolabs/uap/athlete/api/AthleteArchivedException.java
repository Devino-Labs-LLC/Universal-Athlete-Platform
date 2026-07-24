package com.devinolabs.uap.athlete.api;

public class AthleteArchivedException extends RuntimeException {

	public AthleteArchivedException() {
		super("Archived athlete cannot be modified");
	}

}
