package com.devinolabs.uap.billing.application;

public class BillingAccountNotFoundException extends RuntimeException {

	public BillingAccountNotFoundException() {
		super("Account billing was not found");
	}

}
