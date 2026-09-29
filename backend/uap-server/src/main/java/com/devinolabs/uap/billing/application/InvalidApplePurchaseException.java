package com.devinolabs.uap.billing.application;

/**
 * Apple signed transaction or notification verification failed. HTTP maps this to a generic 400.
 */
public class InvalidApplePurchaseException extends RuntimeException {

	public InvalidApplePurchaseException(String message) {
		super(message == null || message.isBlank() ? "Invalid Apple purchase" : message);
	}

	public InvalidApplePurchaseException(String message, Throwable cause) {
		super(message == null || message.isBlank() ? "Invalid Apple purchase" : message, cause);
	}

}
