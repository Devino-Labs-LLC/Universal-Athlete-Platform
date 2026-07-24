package com.devinolabs.uap.identity.domain;

import java.util.Objects;
import java.util.UUID;

public final class EmailVerificationTokenId {

	private final UUID value;

	private EmailVerificationTokenId(UUID value) {
		this.value = Objects.requireNonNull(value, "EmailVerificationTokenId value must not be null");
	}

	public static EmailVerificationTokenId generate() {
		return new EmailVerificationTokenId(UUID.randomUUID());
	}

	public static EmailVerificationTokenId of(UUID value) {
		return new EmailVerificationTokenId(value);
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof EmailVerificationTokenId tokenId)) {
			return false;
		}
		return value.equals(tokenId.value);
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
