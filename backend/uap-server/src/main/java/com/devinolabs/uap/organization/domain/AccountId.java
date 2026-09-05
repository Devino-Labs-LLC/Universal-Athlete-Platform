package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Cross-context reference to an Identity account.
 * Local value object only — no dependency on the Identity module.
 */
public final class AccountId {

	private final UUID value;

	private AccountId(UUID value) {
		this.value = Objects.requireNonNull(value, "AccountId value must not be null");
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

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AccountId accountId)) {
			return false;
		}
		return value.equals(accountId.value);
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
