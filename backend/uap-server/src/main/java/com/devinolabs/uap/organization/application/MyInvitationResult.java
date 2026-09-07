package com.devinolabs.uap.organization.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

public record MyInvitationResult(
		UUID id,
		UUID organizationId,
		String organizationName,
		UUID teamId,
		String teamName,
		OrganizationMembershipRole role,
		Instant expiresAt) {

	public MyInvitationResult {
		Objects.requireNonNull(id);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(organizationName);
		Objects.requireNonNull(role);
		Objects.requireNonNull(expiresAt);
	}

}
