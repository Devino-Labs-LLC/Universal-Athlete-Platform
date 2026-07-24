package com.devinolabs.uap.identity.domain;

public class TokenAlreadyConsumedException extends RuntimeException {

	public TokenAlreadyConsumedException() {
		super("Email verification token has already been consumed");
	}

}
