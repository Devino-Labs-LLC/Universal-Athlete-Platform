package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

public final class OrganizationId {

	private final UUID value;

	private OrganizationId(UUID value) {
		this.value = Objects.requireNonNull(value, "OrganizationId value must not be null");
	}

	public static OrganizationId generate() {
		return new OrganizationId(UUID.randomUUID());
	}

	public static OrganizationId of(UUID value) {
		return new OrganizationId(value);
	}

	public static OrganizationId of(String value) {
		Objects.requireNonNull(value, "OrganizationId value must not be null");
		return new OrganizationId(UUID.fromString(value));
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof OrganizationId organizationId)) {
			return false;
		}
		return value.equals(organizationId.value);
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
