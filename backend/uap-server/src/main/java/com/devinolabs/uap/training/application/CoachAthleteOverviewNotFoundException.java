package com.devinolabs.uap.training.application;

/**
 * Thrown when a coach athlete overview target is inaccessible (404, no existence leak).
 */
public class CoachAthleteOverviewNotFoundException extends RuntimeException {

	public CoachAthleteOverviewNotFoundException() {
		super("Coach athlete overview not found");
	}

}
