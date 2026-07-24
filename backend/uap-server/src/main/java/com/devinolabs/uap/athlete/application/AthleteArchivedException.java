package com.devinolabs.uap.athlete.application;

public class AthleteArchivedException extends RuntimeException {

	public AthleteArchivedException() {
		super("Archived athlete cannot be modified");
	}

}
