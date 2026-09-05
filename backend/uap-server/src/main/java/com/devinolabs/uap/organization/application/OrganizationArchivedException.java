package com.devinolabs.uap.organization.application;

public class OrganizationArchivedException extends RuntimeException {

	public OrganizationArchivedException() {
		super("Archived organization cannot be modified");
	}

}
