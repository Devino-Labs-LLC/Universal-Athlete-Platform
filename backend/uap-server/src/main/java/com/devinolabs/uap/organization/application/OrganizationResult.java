package com.devinolabs.uap.organization.application;

import java.time.Instant;

import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationStatus;

public record OrganizationResult(
		OrganizationId id,
		String name,
		OrganizationStatus status,
		Instant createdAt,
		Instant updatedAt,
		long version) {

	public static OrganizationResult from(Organization organization) {
		return new OrganizationResult(
				organization.id(),
				organization.name(),
				organization.status(),
				organization.createdAt(),
				organization.updatedAt(),
				organization.version());
	}

}
