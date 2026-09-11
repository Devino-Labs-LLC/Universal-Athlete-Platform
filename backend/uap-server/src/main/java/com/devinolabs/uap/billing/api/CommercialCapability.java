package com.devinolabs.uap.billing.api;

/**
 * Provider-neutral commercial capabilities (Slice A vocabulary).
 *
 * <p>Enforcement at V3 product edges is deferred to Slice C. Free privacy/account-control
 * surfaces are never represented here and must never be paywalled.
 */
public enum CommercialCapability {

	ORG_TEAM_MANAGEMENT,
	ORG_COACH_COLLABORATION,
	ORG_COACH_ATHLETE_VIEW,
	ORG_TEAM_READINESS,
	INDIVIDUAL_PREMIUM

}
