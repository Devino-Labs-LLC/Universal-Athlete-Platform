package com.devinolabs.uap.identity.domain;

import java.util.Objects;
import java.util.UUID;

public final class RefreshSessionId {

	private final UUID value;

	private RefreshSessionId(UUID value) {
		this.value = Objects.requireNonNull(value, "RefreshSessionId value must not be null");
	}

	public static RefreshSessionId generate() {
		return new RefreshSessionId(UUID.randomUUID());
	}

	public static RefreshSessionId of(UUID value) {
		return new RefreshSessionId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof RefreshSessionId sessionId)) {
			return false;
		}
		return value.equals(sessionId.value);
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
