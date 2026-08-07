package com.devinolabs.uap.training.application;

public class DailyReadinessStateSnapshotRequiredException extends RuntimeException {

	public DailyReadinessStateSnapshotRequiredException(String message) {
		super(message);
	}

	public DailyReadinessStateSnapshotRequiredException(String message, Throwable cause) {
		super(message, cause);
	}

}
