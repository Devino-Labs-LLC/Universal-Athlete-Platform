package com.devinolabs.uap.consent.application;

public class ConsentNotFoundException extends RuntimeException {

	public ConsentNotFoundException() {
		super("Consent grant was not found");
	}

}
