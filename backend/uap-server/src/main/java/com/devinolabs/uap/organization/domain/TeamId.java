package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

public record TeamId(UUID value) {

	public TeamId {
		Objects.requireNonNull(value, "TeamId value must not be null");
	}

	public static TeamId generate() {
		return new TeamId(UUID.randomUUID());
	}

	public static TeamId of(UUID value) {
		return new TeamId(value);
	}

	public static TeamId of(String value) {
		Objects.requireNonNull(value, "TeamId value must not be null");
		return new TeamId(UUID.fromString(value));
	}

}
