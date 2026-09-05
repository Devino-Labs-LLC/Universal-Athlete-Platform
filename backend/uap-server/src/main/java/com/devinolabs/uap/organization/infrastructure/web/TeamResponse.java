package com.devinolabs.uap.organization.infrastructure.web;

import java.time.Instant;

import com.devinolabs.uap.organization.application.TeamResult;

public record TeamResponse(
		String id,
		String organizationId,
		String name,
		String status,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	static TeamResponse from(TeamResult result) {
		return new TeamResponse(
				result.id().value().toString(),
				result.organizationId().value().toString(),
				result.name(),
				result.status().name(),
				result.createdAt(),
				result.updatedAt(),
				result.version());
	}

}
