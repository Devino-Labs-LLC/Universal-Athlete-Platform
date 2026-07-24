package com.devinolabs.uap.identity.application;

public class RevokedRefreshTokenException extends RuntimeException {

	public RevokedRefreshTokenException() {
		super("Refresh token has been revoked or replayed");
	}

}
