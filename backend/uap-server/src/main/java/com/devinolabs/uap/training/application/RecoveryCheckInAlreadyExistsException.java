package com.devinolabs.uap.training.application;

public class RecoveryCheckInAlreadyExistsException extends RuntimeException {

	public RecoveryCheckInAlreadyExistsException() {
		super("A recovery check-in already exists for this date");
	}

}
