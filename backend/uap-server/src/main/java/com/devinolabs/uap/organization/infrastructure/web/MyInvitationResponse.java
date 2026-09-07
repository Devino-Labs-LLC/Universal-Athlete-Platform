package com.devinolabs.uap.organization.infrastructure.web;

import java.util.Objects;

import com.devinolabs.uap.organization.application.MyInvitationResult;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

record MyInvitationResponse(
		String id,
		String organizationId,
		String organizationName,
		String teamId,
		String teamName,
		OrganizationMembershipRole role,
		String expiresAt) {

	static MyInvitationResponse from(MyInvitationResult result) {
		Objects.requireNonNull(result);
		return new MyInvitationResponse(
				result.id().toString(),
				result.organizationId().toString(),
				result.organizationName(),
				result.teamId() == null ? null : result.teamId().toString(),
				result.teamName(),
				result.role(),
				result.expiresAt().toString());
	}

}
