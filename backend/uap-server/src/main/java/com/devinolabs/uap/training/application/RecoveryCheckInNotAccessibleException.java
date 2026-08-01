package com.devinolabs.uap.training.application;

public class RecoveryCheckInNotAccessibleException extends RuntimeException {

	public RecoveryCheckInNotAccessibleException() {
		super("Recovery check-in is not accessible");
	}

}
