package com.devinolabs.uap.billing.domain;

/**
 * Domain identifiers for billing channels. No SDKs or network clients in Slice A.
 */
public enum BillingProvider {

	STRIPE,
	APPLE_APP_STORE,
	GOOGLE_PLAY

}
