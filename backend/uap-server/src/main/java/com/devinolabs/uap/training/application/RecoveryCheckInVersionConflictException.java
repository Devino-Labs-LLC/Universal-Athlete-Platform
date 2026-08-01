package com.devinolabs.uap.training.application;

public class RecoveryCheckInVersionConflictException extends RuntimeException {

	public RecoveryCheckInVersionConflictException() {
		super("Recovery check-in version conflict");
	}

}
