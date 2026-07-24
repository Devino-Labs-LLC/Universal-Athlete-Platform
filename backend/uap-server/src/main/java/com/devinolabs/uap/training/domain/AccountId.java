package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

/** Local account identifier — no dependency on Identity domain types. */
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
