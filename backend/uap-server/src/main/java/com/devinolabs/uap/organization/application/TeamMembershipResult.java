package com.devinolabs.uap.organization.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;
import com.devinolabs.uap.organization.domain.TeamMembership;

public record TeamMembershipResult(
		UUID id,
		UUID teamId,
		UUID accountId,
		UUID athleteId,
		OrganizationMembershipRole role,
		OrganizationMembershipStatus status,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	public TeamMembershipResult {
		Objects.requireNonNull(id);
		Objects.requireNonNull(teamId);
		Objects.requireNonNull(accountId);
		Objects.requireNonNull(role);
		Objects.requireNonNull(status);
		Objects.requireNonNull(createdAt);
		Objects.requireNonNull(updatedAt);
	}

	public static TeamMembershipResult from(TeamMembership membership) {
		return new TeamMembershipResult(
				membership.id().value(),
				membership.teamId().value(),
				membership.accountId().value(),
				membership.athleteId(),
				membership.role(),
				membership.status(),
				membership.createdAt(),
				membership.updatedAt(),
				membership.version());
	}

}
