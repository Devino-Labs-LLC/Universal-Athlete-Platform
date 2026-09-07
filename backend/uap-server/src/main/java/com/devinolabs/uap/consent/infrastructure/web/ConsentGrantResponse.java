package com.devinolabs.uap.consent.infrastructure.web;

import java.util.List;
import java.util.Objects;

import com.devinolabs.uap.consent.application.ConsentGrantResult;

record ConsentGrantResponse(
		String id,
		String athleteId,
		String teamId,
		String organizationId,
		String teamMembershipId,
		List<String> scopes,
		String status,
		String createdAt,
		String revokedAt,
		String updatedAt,
		long version,
		String teamName,
		String organizationName) {

	static ConsentGrantResponse from(ConsentGrantResult result) {
		Objects.requireNonNull(result, "result must not be null");
		return new ConsentGrantResponse(
				result.id().toString(),
				result.athleteId().toString(),
				result.teamId().toString(),
				result.organizationId().toString(),
				result.teamMembershipId().toString(),
				List.copyOf(result.scopes()),
				result.status().name(),
				result.createdAt().toString(),
				result.revokedAt() == null ? null : result.revokedAt().toString(),
				result.updatedAt().toString(),
				result.version(),
				result.teamName(),
				result.organizationName());
	}

}
