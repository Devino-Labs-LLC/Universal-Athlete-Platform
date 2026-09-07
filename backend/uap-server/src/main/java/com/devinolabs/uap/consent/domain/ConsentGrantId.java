package com.devinolabs.uap.consent.domain;

import java.util.Objects;
import java.util.UUID;

public record ConsentGrantId(UUID value) {

	public ConsentGrantId {
		Objects.requireNonNull(value, "ConsentGrantId value must not be null");
	}

	public static ConsentGrantId generate() {
		return new ConsentGrantId(UUID.randomUUID());
	}

	public static ConsentGrantId of(UUID value) {
		return new ConsentGrantId(value);
	}

	public static ConsentGrantId of(String value) {
		Objects.requireNonNull(value, "ConsentGrantId value must not be null");
		return new ConsentGrantId(UUID.fromString(value));
	}

}
