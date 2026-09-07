package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

public record TeamMembershipId(UUID value) {

	public TeamMembershipId {
		Objects.requireNonNull(value, "TeamMembershipId value must not be null");
	}

	public static TeamMembershipId generate() {
		return new TeamMembershipId(UUID.randomUUID());
	}

	public static TeamMembershipId of(UUID value) {
		return new TeamMembershipId(value);
	}

	public static TeamMembershipId of(String value) {
		Objects.requireNonNull(value, "TeamMembershipId value must not be null");
		return new TeamMembershipId(UUID.fromString(value));
	}

}
