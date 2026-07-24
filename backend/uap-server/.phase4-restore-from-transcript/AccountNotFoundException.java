package com.devinolabs.uap.identity.application;

public class AccountNotFoundException extends RuntimeException {

	public AccountNotFoundException() {
		super("Account was not found");
	}

}
