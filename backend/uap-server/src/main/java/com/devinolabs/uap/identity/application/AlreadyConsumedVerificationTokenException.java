package com.devinolabs.uap.identity.application;

public class AlreadyConsumedVerificationTokenException extends RuntimeException {

	public AlreadyConsumedVerificationTokenException() {
		super("Email verification token has already been consumed");
	}

	public AlreadyConsumedVerificationTokenException(Throwable cause) {
		super("Email verification token has already been consumed", cause);
	}

}
