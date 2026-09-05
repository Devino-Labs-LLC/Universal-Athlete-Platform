package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Cross-context reference to an Identity account.
 * Local value object only — no dependency on the Identity module.
 */
public record AccountId(UUID value) {

	public AccountId {
		Objects.requireNonNull(value, "AccountId value must not be null");
	}

	public static AccountId generate() {
		return new AccountId(UUID.randomUUID());
	}

	public static AccountId of(UUID value) {
		return new AccountId(value);
	}

	public static AccountId of(String value) {
		Objects.requireNonNull(value, "AccountId value must not be null");
		return new AccountId(UUID.fromString(value));
	}

}
