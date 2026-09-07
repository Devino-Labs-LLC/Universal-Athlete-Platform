package com.devinolabs.uap.organization.infrastructure.web;

import java.util.Objects;

import com.devinolabs.uap.organization.application.TeamMembershipResult;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;

record TeamMembershipResponse(
		String id,
		String teamId,
		String accountId,
		String athleteId,
		OrganizationMembershipRole role,
		OrganizationMembershipStatus status,
		String createdAt,
		String updatedAt,
		long version) {

	static TeamMembershipResponse from(TeamMembershipResult result) {
		Objects.requireNonNull(result);
		return new TeamMembershipResponse(
				result.id().toString(),
				result.teamId().toString(),
				result.accountId().toString(),
				result.athleteId() == null ? null : result.athleteId().toString(),
				result.role(),
				result.status(),
				result.createdAt().toString(),
				result.updatedAt().toString(),
				result.version());
	}

}
