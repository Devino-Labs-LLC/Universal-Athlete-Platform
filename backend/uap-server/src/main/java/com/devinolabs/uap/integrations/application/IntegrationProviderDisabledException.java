package com.devinolabs.uap.integrations.application;

import java.util.Objects;

public class IntegrationProviderDisabledException extends RuntimeException {

	private final String code;

	public IntegrationProviderDisabledException(String code, String message) {
		super(message);
		this.code = Objects.requireNonNull(code, "code must not be null");
	}

	public String code() {
		return code;
	}

}
