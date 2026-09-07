package com.devinolabs.uap.organization.application;

public class InvitationNotFoundException extends RuntimeException {

	public InvitationNotFoundException() {
		super("Invitation was not found");
	}

}
