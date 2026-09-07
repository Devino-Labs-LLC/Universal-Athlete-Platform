package com.devinolabs.uap.organization.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;

public record OrganizationMembershipResult(
		UUID id,
		UUID organizationId,
		UUID accountId,
		UUID athleteId,
		OrganizationMembershipRole role,
		OrganizationMembershipStatus status,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	public OrganizationMembershipResult {
		Objects.requireNonNull(id);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(accountId);
		Objects.requireNonNull(role);
		Objects.requireNonNull(status);
		Objects.requireNonNull(createdAt);
		Objects.requireNonNull(updatedAt);
	}

	public static OrganizationMembershipResult from(OrganizationMembership membership) {
		return new OrganizationMembershipResult(
				membership.id().value(),
				membership.organizationId().value(),
				membership.accountId().value(),
				membership.athleteId(),
				membership.role(),
				membership.status(),
				membership.createdAt(),
				membership.updatedAt(),
				membership.version());
	}

}
