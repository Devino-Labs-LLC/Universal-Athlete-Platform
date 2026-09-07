package com.devinolabs.uap.organization.domain;

import java.util.Objects;

/**
 * Invitation aggregate plus the raw token returned once at issue time.
 */
public record IssuedInvitation(Invitation invitation, String rawToken) {

	public IssuedInvitation {
		Objects.requireNonNull(invitation, "invitation must not be null");
		Objects.requireNonNull(rawToken, "rawToken must not be null");
		if (rawToken.isBlank()) {
			throw new IllegalArgumentException("rawToken must not be blank");
		}
	}

}
