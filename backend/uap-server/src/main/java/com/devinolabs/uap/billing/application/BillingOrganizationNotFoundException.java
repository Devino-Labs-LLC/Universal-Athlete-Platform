package com.devinolabs.uap.billing.application;

public class BillingOrganizationNotFoundException extends RuntimeException {

	public BillingOrganizationNotFoundException() {
		super("Organization was not found");
	}

}
