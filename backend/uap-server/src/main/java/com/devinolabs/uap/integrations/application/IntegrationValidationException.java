package com.devinolabs.uap.integrations.application;

import java.util.Objects;

public class IntegrationValidationException extends RuntimeException {

	private final String code;

	public IntegrationValidationException(String code, String message) {
		super(message);
		this.code = Objects.requireNonNull(code, "code must not be null");
	}

	public String code() {
		return code;
	}
}
