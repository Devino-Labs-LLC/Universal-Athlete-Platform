package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Invitation;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.InvitationStatus;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;

@Service
public class RevokeInvitationUseCase {

	private final InvitationRepository invitationRepository;
	private final TeamAccessGuard teamAccessGuard;
	private final OrganizationAccessGuard organizationAccessGuard;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public RevokeInvitationUseCase(
			InvitationRepository invitationRepository,
			TeamAccessGuard teamAccessGuard,
			OrganizationAccessGuard organizationAccessGuard,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.invitationRepository = Objects.requireNonNull(invitationRepository);
		this.teamAccessGuard = Objects.requireNonNull(teamAccessGuard);
		this.organizationAccessGuard = Objects.requireNonNull(organizationAccessGuard);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void revokeTeamInvitation(AccountId actorAccountId, TeamId teamId, InvitationId invitationId) {
		Team team = teamAccessGuard.requireActiveTeam(actorAccountId, teamId);
		Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
				.orElseThrow(InvitationNotFoundException::new);
		if (invitation.teamId() == null || !invitation.teamId().equals(teamId)) {
			throw new InvitationNotFoundException();
		}
		teamAccessGuard.requireInviteCapability(actorAccountId, team, invitation.role());
		revoke(actorAccountId, invitation);
	}

	@Transactional
	public void revokeOrganizationInvitation(
			AccountId actorAccountId,
			OrganizationId organizationId,
			InvitationId invitationId) {
		organizationAccessGuard.requireOrgAdminOrOwner(actorAccountId, organizationId);
		Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
				.orElseThrow(InvitationNotFoundException::new);
		if (!invitation.organizationId().equals(organizationId) || invitation.teamId() != null) {
			throw new InvitationNotFoundException();
		}
		revoke(actorAccountId, invitation);
	}

	private void revoke(AccountId actorAccountId, Invitation invitation) {
		if (invitation.status() == InvitationStatus.REVOKED) {
			return;
		}
		if (invitation.status() != InvitationStatus.PENDING) {
			throw new InvitationNotFoundException();
		}
		if (invitation.isExpired(clock)) {
			invitation.markExpired(clock);
			invitationRepository.save(invitation);
			throw new InvitationNotFoundException();
		}
		invitation.revoke(clock);
		invitationRepository.save(invitation);
		auditPort.invitationRevoked(invitation.id(), invitation.organizationId(), actorAccountId);
	}

}
