package com.devinolabs.uap.identity.application;

public class AccountDisabledException extends RuntimeException {

	public AccountDisabledException() {
		super("Account is disabled");
	}

}
