package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

public record OrganizationId(UUID value) {

	public OrganizationId {
		Objects.requireNonNull(value, "OrganizationId value must not be null");
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

}
