package com.devinolabs.uap.entitlements;

/**
 * Provider-neutral commercial capabilities (Slice A vocabulary).
 *
 * <p>Organization capabilities are enforced at approved product edges when
 * {@code UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED} is true. Free privacy/account-control
 * surfaces are never represented here and must never be paywalled.
 * {@code INDIVIDUAL_PREMIUM} is not enforced in Slice C.
 */
public enum CommercialCapability {

	ORG_TEAM_MANAGEMENT,
	ORG_COACH_COLLABORATION,
	ORG_COACH_ATHLETE_VIEW,
	ORG_TEAM_READINESS,
	INDIVIDUAL_PREMIUM

}
