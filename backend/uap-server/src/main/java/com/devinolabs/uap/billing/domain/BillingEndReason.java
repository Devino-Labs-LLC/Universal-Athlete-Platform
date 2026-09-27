package com.devinolabs.uap.billing.domain;

/**
 * Bounded reason for {@code BILLING_SUBSCRIPTION_ENDED}. Delinquency is the only
 * reason that is recorded. Abandoned checkout and voluntary period end stay unspecified.
 */
public enum BillingEndReason {

	UNSPECIFIED,
	NONPAYMENT

}
