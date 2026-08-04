package com.devinolabs.uap.training.application;

public class DailyAthleteStateSnapshotNotFoundException extends RuntimeException {

	public DailyAthleteStateSnapshotNotFoundException(String message) {
		super(message);
	}

	public DailyAthleteStateSnapshotNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}
