package com.devinolabs.uap.organization.infrastructure.web;

import java.util.Objects;

import com.devinolabs.uap.organization.application.OrganizationMembershipResult;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;

record OrganizationMembershipResponse(
		String id,
		String organizationId,
		String accountId,
		String athleteId,
		OrganizationMembershipRole role,
		OrganizationMembershipStatus status,
		String createdAt,
		String updatedAt,
		long version) {

	static OrganizationMembershipResponse from(OrganizationMembershipResult result) {
		Objects.requireNonNull(result);
		return new OrganizationMembershipResponse(
				result.id().toString(),
				result.organizationId().toString(),
				result.accountId().toString(),
				result.athleteId() == null ? null : result.athleteId().toString(),
				result.role(),
				result.status(),
				result.createdAt().toString(),
				result.updatedAt().toString(),
				result.version());
	}

}
