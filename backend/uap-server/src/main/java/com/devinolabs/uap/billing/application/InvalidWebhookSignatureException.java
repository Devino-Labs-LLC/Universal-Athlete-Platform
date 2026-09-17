package com.devinolabs.uap.billing.application;

/**
 * Stripe signature verification failed. HTTP layer maps this to a generic 400.
 */
public class InvalidWebhookSignatureException extends RuntimeException {

	public InvalidWebhookSignatureException() {
		super("Invalid webhook signature");
	}

}
