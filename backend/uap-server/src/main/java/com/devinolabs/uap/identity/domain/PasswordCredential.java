package com.devinolabs.uap.identity.domain;

import java.util.Objects;

public final class PasswordCredential {

	private final String hash;

	private PasswordCredential(String hash) {
		this.hash = hash;
	}

	public static PasswordCredential fromHash(String hash) {
		if (hash == null || hash.isBlank()) {
			throw new IllegalArgumentException("Password hash must not be blank");
		}
		return new PasswordCredential(hash);
	}

	public String hash() {
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PasswordCredential credential)) {
			return false;
		}
		return hash.equals(credential.hash);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(hash);
	}

	@Override
	public String toString() {
		return "PasswordCredential[****]";
	}

}
