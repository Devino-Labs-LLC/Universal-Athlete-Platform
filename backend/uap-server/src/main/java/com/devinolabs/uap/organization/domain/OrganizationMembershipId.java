package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

public final class OrganizationMembershipId {

	private final UUID value;

	private OrganizationMembershipId(UUID value) {
		this.value = Objects.requireNonNull(value, "OrganizationMembershipId value must not be null");
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

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof OrganizationMembershipId membershipId)) {
			return false;
		}
		return value.equals(membershipId.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
