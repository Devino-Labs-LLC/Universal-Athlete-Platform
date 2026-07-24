package com.devinolabs.uap.identity.application;

public class EmailNotVerifiedException extends RuntimeException {

	public EmailNotVerifiedException() {
		super("Email address has not been verified");
	}

}
