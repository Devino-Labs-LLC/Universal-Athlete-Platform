package com.devinolabs.uap.organization.infrastructure.web;

import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.organization.application.InvitationResult;
import com.devinolabs.uap.organization.domain.InvitationStatus;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

record InvitationResponse(
		String id,
		String organizationId,
		String teamId,
		String invitedEmail,
		String invitedAccountId,
		OrganizationMembershipRole role,
		InvitationStatus status,
		String expiresAt,
		String acceptedMembershipId,
		String createdByAccountId,
		String createdAt,
		String updatedAt,
		long version,
		String rawToken) {

	static InvitationResponse from(InvitationResult result) {
		Objects.requireNonNull(result);
		return new InvitationResponse(
				result.id().toString(),
				result.organizationId().toString(),
				result.teamId() == null ? null : result.teamId().toString(),
				result.invitedEmail(),
				result.invitedAccountId() == null ? null : result.invitedAccountId().toString(),
				result.role(),
				result.status(),
				result.expiresAt().toString(),
				result.acceptedMembershipId() == null ? null : result.acceptedMembershipId().toString(),
				result.createdByAccountId().toString(),
				result.createdAt().toString(),
				result.updatedAt().toString(),
				result.version(),
				result.rawToken());
	}

	static InvitationResponse withoutToken(InvitationResult result) {
		InvitationResponse full = from(result);
		return new InvitationResponse(
				full.id(),
				full.organizationId(),
				full.teamId(),
				full.invitedEmail(),
				full.invitedAccountId(),
				full.role(),
				full.status(),
				full.expiresAt(),
				full.acceptedMembershipId(),
				full.createdByAccountId(),
				full.createdAt(),
				full.updatedAt(),
				full.version(),
				null);
	}

}
