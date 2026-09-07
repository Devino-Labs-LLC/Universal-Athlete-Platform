package com.devinolabs.uap.identity.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Published read-only account directory projection for other modules.
 */
public record AccountDirectoryRef(UUID accountId, String normalizedEmail, boolean emailVerified) {

	public AccountDirectoryRef {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(normalizedEmail, "normalizedEmail must not be null");
		if (normalizedEmail.isBlank()) {
			throw new IllegalArgumentException("normalizedEmail must not be blank");
		}
	}

}
