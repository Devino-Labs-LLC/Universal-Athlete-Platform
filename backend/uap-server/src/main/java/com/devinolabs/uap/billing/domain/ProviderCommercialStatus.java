package com.devinolabs.uap.billing.domain;

/**
 * Provider-neutral commercial status mapped into the canonical lifecycle (ADR-040).
 * Provider adapters translate their native status vocabulary into this minimal set.
 */
public enum ProviderCommercialStatus {

	PENDING,
	TRIALING,
	ACTIVE,
	PAYMENT_ATTENTION_REQUIRED,
	ENDED,
	UNKNOWN

}
