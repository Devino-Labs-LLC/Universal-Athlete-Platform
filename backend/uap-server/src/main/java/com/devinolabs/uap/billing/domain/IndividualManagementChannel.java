package com.devinolabs.uap.billing.domain;

/**
 * Where Account Individual Premium management must be performed (ADR-045 / §9).
 * No silent credential copy across providers.
 */
public enum IndividualManagementChannel {

	STRIPE_CUSTOMER_PORTAL,
	APPLE_APP_STORE,
	GOOGLE_PLAY;

	public static IndividualManagementChannel forProvider(BillingProvider provider) {
		return switch (provider) {
			case STRIPE -> STRIPE_CUSTOMER_PORTAL;
			case APPLE_APP_STORE -> APPLE_APP_STORE;
			case GOOGLE_PLAY -> GOOGLE_PLAY;
		};
	}
}
