package com.devinolabs.uap.organization.application;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.InvitationAuthority;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamStatus;

/**
 * Team-scoped access checks. Denied access throws TeamNotFoundException (404).
 */
@Service
public class TeamAccessGuard {

	private final TeamRepository teamRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final OrganizationMembershipRepository organizationMembershipRepository;

	public TeamAccessGuard(
			TeamRepository teamRepository,
			TeamMembershipRepository teamMembershipRepository,
			OrganizationMembershipRepository organizationMembershipRepository) {
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.organizationMembershipRepository = Objects.requireNonNull(organizationMembershipRepository);
	}

	public Team requireVisibleTeam(AccountId accountId, TeamId teamId) {
		Team team = teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);
		if (hasTeamOrOrgAccess(accountId, team)) {
			return team;
		}
		throw new TeamNotFoundException();
	}

	public Team requireActiveTeam(AccountId accountId, TeamId teamId) {
		Team team = requireVisibleTeam(accountId, teamId);
		if (team.status() == TeamStatus.ARCHIVED) {
			throw new TeamArchivedException();
		}
		return team;
	}

	public ActorCapability requireInviteCapability(AccountId accountId, Team team, OrganizationMembershipRole invitedRole) {
		Optional<OrganizationMembership> orgMembership = organizationMembershipRepository
				.findActiveByOrganizationIdAndAccountId(team.organizationId(), accountId);
		if (orgMembership.isPresent()) {
			OrganizationMembership membership = orgMembership.get();
			if (InvitationAuthority.canInviteTeamRole(membership.role(), true, invitedRole)) {
				return new ActorCapability(membership.role(), true, null, membership);
			}
			throw new TeamNotFoundException();
		}
		TeamMembership teamMembership = teamMembershipRepository.findActiveByTeamIdAndAccountId(team.id(), accountId)
				.orElseThrow(TeamNotFoundException::new);
		if (!InvitationAuthority.canInviteTeamRole(teamMembership.role(), false, invitedRole)) {
			throw new TeamNotFoundException();
		}
		return new ActorCapability(teamMembership.role(), false, teamMembership, null);
	}

	public ActorCapability requireListInvitationsCapability(AccountId accountId, Team team) {
		Optional<OrganizationMembership> orgMembership = organizationMembershipRepository
				.findActiveByOrganizationIdAndAccountId(team.organizationId(), accountId);
		if (orgMembership.isPresent()) {
			OrganizationMembership membership = orgMembership.get();
			if (InvitationAuthority.canListTeamInvitations(membership.role(), true)) {
				return new ActorCapability(membership.role(), true, null, membership);
			}
			throw new TeamNotFoundException();
		}
		TeamMembership teamMembership = teamMembershipRepository.findActiveByTeamIdAndAccountId(team.id(), accountId)
				.orElseThrow(TeamNotFoundException::new);
		if (!InvitationAuthority.canListTeamInvitations(teamMembership.role(), false)) {
			throw new TeamNotFoundException();
		}
		return new ActorCapability(teamMembership.role(), false, teamMembership, null);
	}

	public ActorCapability requireManageRoster(AccountId accountId, Team team) {
		Optional<OrganizationMembership> orgMembership = organizationMembershipRepository
				.findActiveByOrganizationIdAndAccountId(team.organizationId(), accountId);
		if (orgMembership.isPresent()) {
			OrganizationMembership membership = orgMembership.get();
			if (InvitationAuthority.canManageTeamRoster(membership.role(), true)) {
				return new ActorCapability(membership.role(), true, null, membership);
			}
			throw new TeamNotFoundException();
		}
		TeamMembership teamMembership = teamMembershipRepository.findActiveByTeamIdAndAccountId(team.id(), accountId)
				.orElseThrow(TeamNotFoundException::new);
		if (!InvitationAuthority.canManageTeamRoster(teamMembership.role(), false)) {
			throw new TeamNotFoundException();
		}
		return new ActorCapability(teamMembership.role(), false, teamMembership, null);
	}

	public boolean hasTeamOrOrgAccess(AccountId accountId, Team team) {
		if (teamMembershipRepository.existsActiveMembership(accountId, team.id())) {
			return true;
		}
		return organizationMembershipRepository.existsActiveMembership(accountId, team.organizationId());
	}

	public record ActorCapability(
			OrganizationMembershipRole role,
			boolean orgScoped,
			TeamMembership teamMembership,
			OrganizationMembership organizationMembership) {
	}

}
