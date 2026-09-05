package com.devinolabs.uap.organization.application;

public class TeamArchivedException extends RuntimeException {

	public TeamArchivedException() {
		super("Archived team cannot be modified");
	}

}
