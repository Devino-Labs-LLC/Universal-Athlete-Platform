package com.devinolabs.uap.integrations.domain;

import java.util.Objects;
import java.util.UUID;

public record SyncRunId(UUID value) {

	public SyncRunId {
		Objects.requireNonNull(value, "SyncRunId value must not be null");
	}

	public static SyncRunId generate() {
		return new SyncRunId(UUID.randomUUID());
	}

	public static SyncRunId of(UUID value) {
		return new SyncRunId(value);
	}

}
