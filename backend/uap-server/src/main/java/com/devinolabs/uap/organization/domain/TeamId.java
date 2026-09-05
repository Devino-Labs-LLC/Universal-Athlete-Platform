package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

public final class TeamId {

	private final UUID value;

	private TeamId(UUID value) {
		this.value = Objects.requireNonNull(value, "TeamId value must not be null");
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

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof TeamId teamId)) {
			return false;
		}
		return value.equals(teamId.value);
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
