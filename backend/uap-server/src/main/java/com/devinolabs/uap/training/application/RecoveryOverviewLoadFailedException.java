package com.devinolabs.uap.training.application;

public class RecoveryOverviewLoadFailedException extends RuntimeException {

	public RecoveryOverviewLoadFailedException(String message) {
		super(message);
	}

	public RecoveryOverviewLoadFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
