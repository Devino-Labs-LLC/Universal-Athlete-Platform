package com.devinolabs.uap.billing.application;

public class BillingProviderUnavailableException extends RuntimeException {

	public BillingProviderUnavailableException(Throwable cause) {
		super("Billing provider request failed", cause);
	}

}
