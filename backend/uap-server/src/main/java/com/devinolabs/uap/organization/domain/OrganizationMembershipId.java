package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

public record OrganizationMembershipId(UUID value) {

	public OrganizationMembershipId {
		Objects.requireNonNull(value, "OrganizationMembershipId value must not be null");
	}

	public static OrganizationMembershipId generate() {
		return new OrganizationMembershipId(UUID.randomUUID());
	}

	public static OrganizationMembershipId of(UUID value) {
		return new OrganizationMembershipId(value);
	}

	public static OrganizationMembershipId of(String value) {
		Objects.requireNonNull(value, "OrganizationMembershipId value must not be null");
		return new OrganizationMembershipId(UUID.fromString(value));
	}

}
