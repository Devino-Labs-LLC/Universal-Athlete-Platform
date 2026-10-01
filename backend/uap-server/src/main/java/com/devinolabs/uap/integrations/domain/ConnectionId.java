package com.devinolabs.uap.integrations.domain;

import java.util.Objects;
import java.util.UUID;

public record ConnectionId(UUID value) {

	public ConnectionId {
		Objects.requireNonNull(value, "ConnectionId value must not be null");
	}

	public static ConnectionId generate() {
		return new ConnectionId(UUID.randomUUID());
	}

	public static ConnectionId of(UUID value) {
		return new ConnectionId(value);
	}

}
