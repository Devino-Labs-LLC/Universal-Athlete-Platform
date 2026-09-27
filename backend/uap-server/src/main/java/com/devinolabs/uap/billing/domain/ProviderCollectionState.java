package com.devinolabs.uap.billing.domain;

/**
 * Provider-neutral collection state for a payment-attention snapshot.
 * Adapters translate native statuses; domain code does not read provider status strings.
 */
public enum ProviderCollectionState {

	NONE,
	PAST_DUE,
	UNPAID,
	PAUSED

}
