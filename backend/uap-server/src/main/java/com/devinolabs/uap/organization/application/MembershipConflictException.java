package com.devinolabs.uap.organization.application;

public class MembershipConflictException extends RuntimeException {

	private final String code;

	public MembershipConflictException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}

}
