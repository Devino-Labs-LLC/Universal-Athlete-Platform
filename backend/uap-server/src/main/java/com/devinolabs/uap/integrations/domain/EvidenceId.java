package com.devinolabs.uap.integrations.domain;

import java.util.Objects;
import java.util.UUID;

public record EvidenceId(UUID value) {

	public EvidenceId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static EvidenceId of(UUID value) {
		return new EvidenceId(value);
	}

	public static EvidenceId generate() {
		return new EvidenceId(UUID.randomUUID());
	}
}
