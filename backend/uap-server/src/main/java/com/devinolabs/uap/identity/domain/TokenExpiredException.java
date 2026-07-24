package com.devinolabs.uap.identity.domain;

public class TokenExpiredException extends RuntimeException {

	public TokenExpiredException() {
		super("Email verification token has expired");
	}

}
