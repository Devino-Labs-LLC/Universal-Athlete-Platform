package com.devinolabs.uap.integrations.application;

public class IntegrationDeviceNotFoundException extends RuntimeException {

	public IntegrationDeviceNotFoundException() {
		super("Integration device was not found");
	}
}
