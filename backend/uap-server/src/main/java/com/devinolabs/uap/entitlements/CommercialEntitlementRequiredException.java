package com.devinolabs.uap.entitlements;

/**
 * Product-edge commercial denial after V3 authorization succeeded.
 * Mapped to HTTP 402 {@code COMMERCIAL_ENTITLEMENT_REQUIRED}.
 */
public class CommercialEntitlementRequiredException extends RuntimeException {

	public static final String CODE = "COMMERCIAL_ENTITLEMENT_REQUIRED";

	public CommercialEntitlementRequiredException() {
		super("The organization does not currently have access to this capability");
	}

}
