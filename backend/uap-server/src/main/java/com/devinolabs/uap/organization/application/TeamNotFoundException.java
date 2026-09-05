package com.devinolabs.uap.organization.application;

public class TeamNotFoundException extends RuntimeException {

	public TeamNotFoundException() {
		super("Team was not found");
	}

}
