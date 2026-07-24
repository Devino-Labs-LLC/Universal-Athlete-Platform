package com.devinolabs.uap.identity.application;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid credentials");
	}

}
