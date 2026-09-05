package com.devinolabs.uap.organization.infrastructure.web;

import java.time.Instant;

import com.devinolabs.uap.organization.application.OrganizationResult;

public record OrganizationResponse(
		String id,
		String name,
		String status,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	static OrganizationResponse from(OrganizationResult result) {
		return new OrganizationResponse(
				result.id().value().toString(),
				result.name(),
				result.status().name(),
				result.createdAt(),
				result.updatedAt(),
				result.version());
	}

}
