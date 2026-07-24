package com.devinolabs.uap.identity.application;

public class ExpiredVerificationTokenException extends RuntimeException {

	public ExpiredVerificationTokenException() {
		super("Email verification token has expired");
	}

	public ExpiredVerificationTokenException(Throwable cause) {
		super("Email verification token has expired", cause);
	}

}
