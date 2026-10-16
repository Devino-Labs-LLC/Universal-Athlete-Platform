package com.devinolabs.uap.integrations.domain;

import java.util.Objects;
import java.util.UUID;

public record DeviceId(UUID value) {

	public DeviceId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static DeviceId of(UUID value) {
		return new DeviceId(value);
	}

	public static DeviceId generate() {
		return new DeviceId(UUID.randomUUID());
	}
}
