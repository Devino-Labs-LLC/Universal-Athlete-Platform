package com.devinolabs.uap.organization.application;

public class InvitationConflictException extends RuntimeException {

	private final String code;

	public InvitationConflictException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}

}
