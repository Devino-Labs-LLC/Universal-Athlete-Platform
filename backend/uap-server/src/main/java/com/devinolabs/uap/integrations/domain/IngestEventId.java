package com.devinolabs.uap.integrations.domain;

import java.util.Objects;
import java.util.UUID;

public record IngestEventId(UUID value) {

	public IngestEventId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static IngestEventId of(UUID value) {
		return new IngestEventId(value);
	}

	public static IngestEventId generate() {
		return new IngestEventId(UUID.randomUUID());
	}
}
