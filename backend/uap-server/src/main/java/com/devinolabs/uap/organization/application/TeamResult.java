package com.devinolabs.uap.organization.application;

import java.time.Instant;

import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamStatus;

public record TeamResult(
		TeamId id,
		OrganizationId organizationId,
		String name,
		TeamStatus status,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	public static TeamResult from(Team team) {
		return new TeamResult(
				team.id(),
				team.organizationId(),
				team.name(),
				team.status(),
				team.createdAt(),
				team.updatedAt(),
				team.version());
	}

}
