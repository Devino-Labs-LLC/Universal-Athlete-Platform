package com.devinolabs.uap.billing.domain;

import com.devinolabs.uap.billing.api.BillingSubjectType;

/**
 * Stable internal catalog keys. No dollar amounts. No provider Price / product IDs.
 */
public enum CommercialPlanKey {

	ORG_BAND_25(BillingSubjectType.ORGANIZATION),
	ORG_BAND_75(BillingSubjectType.ORGANIZATION),
	ORG_BAND_250(BillingSubjectType.ORGANIZATION),
	INDIVIDUAL_PREMIUM(BillingSubjectType.ACCOUNT);

	private final BillingSubjectType requiredSubjectType;

	CommercialPlanKey(BillingSubjectType requiredSubjectType) {
		this.requiredSubjectType = requiredSubjectType;
	}

	public BillingSubjectType requiredSubjectType() {
		return requiredSubjectType;
	}

	public boolean isOrganizationPlan() {
		return requiredSubjectType == BillingSubjectType.ORGANIZATION;
	}

	public boolean isIndividualPlan() {
		return requiredSubjectType == BillingSubjectType.ACCOUNT;
	}

}
