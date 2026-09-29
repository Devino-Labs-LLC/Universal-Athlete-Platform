package com.devinolabs.uap.billing.application;

/**
 * Google Play purchase token or RTDN verification failed. HTTP maps this to a generic 400.
 */
public class InvalidGooglePlayPurchaseException extends RuntimeException {

	public InvalidGooglePlayPurchaseException(String message) {
		super(message == null || message.isBlank() ? "Invalid Google Play purchase" : message);
	}

	public InvalidGooglePlayPurchaseException(String message, Throwable cause) {
		super(message == null || message.isBlank() ? "Invalid Google Play purchase" : message, cause);
	}

}
