package com.devinolabs.uap.identity.application;

public class ExpiredRefreshTokenException extends RuntimeException {

	public ExpiredRefreshTokenException() {
		super("Refresh token has expired");
	}

}
