package com.devinolabs.uap.organization.infrastructure.persistence;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Invitation;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.TeamId;

final class InvitationPersistenceMapper {

	private InvitationPersistenceMapper() {
	}

	static InvitationJpaEntity toEntity(Invitation invitation, boolean isNew) {
		return new InvitationJpaEntity(
				invitation.id().value(),
				invitation.organizationId().value(),
				invitation.teamId() == null ? null : invitation.teamId().value(),
				invitation.invitedEmail(),
				invitation.invitedAccountId() == null ? null : invitation.invitedAccountId().value(),
				invitation.role(),
				invitation.tokenHash(),
				invitation.status(),
				invitation.expiresAt(),
				invitation.acceptedMembershipId(),
				invitation.createdByAccountId().value(),
				invitation.createdAt(),
				invitation.updatedAt(),
				invitation.version(),
				isNew);
	}

	static Invitation toDomain(InvitationJpaEntity entity) {
		return Invitation.rehydrate(
				InvitationId.of(entity.getId()),
				OrganizationId.of(entity.getOrganizationId()),
				entity.getTeamId() == null ? null : TeamId.of(entity.getTeamId()),
				entity.getInvitedEmail(),
				entity.getInvitedAccountId() == null ? null : AccountId.of(entity.getInvitedAccountId()),
				entity.getRole(),
				entity.getTokenHash(),
				entity.getStatus(),
				entity.getExpiresAt(),
				entity.getAcceptedMembershipId(),
				AccountId.of(entity.getCreatedByAccountId()),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
