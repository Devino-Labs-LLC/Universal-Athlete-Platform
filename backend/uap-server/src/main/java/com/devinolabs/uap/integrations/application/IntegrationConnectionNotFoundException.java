package com.devinolabs.uap.integrations.application;

public class IntegrationConnectionNotFoundException extends RuntimeException {

	public IntegrationConnectionNotFoundException() {
		super("Integration connection was not found");
	}

}
