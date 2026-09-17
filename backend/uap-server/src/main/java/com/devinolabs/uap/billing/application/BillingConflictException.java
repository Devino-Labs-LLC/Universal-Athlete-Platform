package com.devinolabs.uap.billing.application;

import java.util.Objects;

public class BillingConflictException extends RuntimeException {

	private final String code;

	public BillingConflictException(String code, String message) {
		super(message);
		this.code = Objects.requireNonNull(code);
	}

	public String code() {
		return code;
	}

}
