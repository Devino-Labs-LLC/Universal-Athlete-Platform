package com.devinolabs.uap.consent.application;

public class ConsentConflictException extends RuntimeException {

	private final String code;

	public ConsentConflictException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}

}
