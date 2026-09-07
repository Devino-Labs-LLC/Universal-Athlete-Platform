package com.devinolabs.uap.organization.domain;

import java.util.Objects;
import java.util.UUID;

public record InvitationId(UUID value) {

	public InvitationId {
		Objects.requireNonNull(value, "InvitationId value must not be null");
	}

	public static InvitationId generate() {
		return new InvitationId(UUID.randomUUID());
	}

	public static InvitationId of(UUID value) {
		return new InvitationId(value);
	}

	public static InvitationId of(String value) {
		Objects.requireNonNull(value, "InvitationId value must not be null");
		return new InvitationId(UUID.fromString(value));
	}

}
