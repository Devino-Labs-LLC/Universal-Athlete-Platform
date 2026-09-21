package com.devinolabs.uap.organization.application;

/**
 * Count-increasing athlete accept denied because an effective Organization band is at or above max,
 * or because effective commercial state is ambiguous. Mapped to HTTP 409.
 * Never used as a 402 entitlement denial.
 */
public class OrganizationAthleteCapacityUnavailableException extends RuntimeException {

	public static final String CODE = "ORGANIZATION_ATHLETE_CAPACITY_UNAVAILABLE";

	public OrganizationAthleteCapacityUnavailableException() {
		super("This organization cannot add another active athlete at this time.");
	}

}
