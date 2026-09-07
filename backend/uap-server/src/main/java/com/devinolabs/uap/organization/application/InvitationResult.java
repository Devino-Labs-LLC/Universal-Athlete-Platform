package com.devinolabs.uap.organization.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.organization.domain.Invitation;
import com.devinolabs.uap.organization.domain.InvitationStatus;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

public record InvitationResult(
		UUID id,
		UUID organizationId,
		UUID teamId,
		String invitedEmail,
		UUID invitedAccountId,
		OrganizationMembershipRole role,
		InvitationStatus status,
		Instant expiresAt,
		UUID acceptedMembershipId,
		UUID createdByAccountId,
		Instant createdAt,
		Instant updatedAt,
		long version,
		String rawToken) {

	public InvitationResult {
		Objects.requireNonNull(id);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(invitedEmail);
		Objects.requireNonNull(role);
		Objects.requireNonNull(status);
		Objects.requireNonNull(expiresAt);
		Objects.requireNonNull(createdByAccountId);
		Objects.requireNonNull(createdAt);
		Objects.requireNonNull(updatedAt);
	}

	public static InvitationResult from(Invitation invitation) {
		return from(invitation, null);
	}

	public static InvitationResult from(Invitation invitation, String rawToken) {
		return new InvitationResult(
				invitation.id().value(),
				invitation.organizationId().value(),
				invitation.teamId() == null ? null : invitation.teamId().value(),
				invitation.invitedEmail(),
				invitation.invitedAccountId() == null ? null : invitation.invitedAccountId().value(),
				invitation.role(),
				invitation.status(),
				invitation.expiresAt(),
				invitation.acceptedMembershipId(),
				invitation.createdByAccountId().value(),
				invitation.createdAt(),
				invitation.updatedAt(),
				invitation.version(),
				rawToken);
	}

}
