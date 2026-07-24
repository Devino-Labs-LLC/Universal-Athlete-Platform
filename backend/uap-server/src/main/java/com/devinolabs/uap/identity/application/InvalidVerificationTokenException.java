package com.devinolabs.uap.identity.application;

public class InvalidVerificationTokenException extends RuntimeException {

	public InvalidVerificationTokenException() {
		super("Email verification token is invalid");
	}

}
