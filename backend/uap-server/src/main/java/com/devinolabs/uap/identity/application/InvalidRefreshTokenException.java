package com.devinolabs.uap.identity.application;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException() {
		super("Refresh token is invalid");
	}

}
