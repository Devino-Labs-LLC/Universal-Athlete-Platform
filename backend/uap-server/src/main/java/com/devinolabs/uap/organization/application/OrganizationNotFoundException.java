package com.devinolabs.uap.organization.application;

public class OrganizationNotFoundException extends RuntimeException {

	public OrganizationNotFoundException() {
		super("Organization was not found");
	}

}
