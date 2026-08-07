package com.devinolabs.uap.training.domain;

public class ReadinessNumericOverflowException extends RuntimeException {

	public ReadinessNumericOverflowException(String message) {
		super(message);
	}

	public ReadinessNumericOverflowException(String message, Throwable cause) {
		super(message, cause);
	}

}
