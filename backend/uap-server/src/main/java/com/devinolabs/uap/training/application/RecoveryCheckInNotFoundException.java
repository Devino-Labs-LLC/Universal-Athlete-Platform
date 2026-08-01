package com.devinolabs.uap.training.application;

public class RecoveryCheckInNotFoundException extends RuntimeException {

	public RecoveryCheckInNotFoundException() {
		super("Recovery check-in was not found");
	}

}
