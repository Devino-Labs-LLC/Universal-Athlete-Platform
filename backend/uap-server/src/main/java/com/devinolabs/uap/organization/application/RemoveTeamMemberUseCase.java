package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.InvitationAuthority;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;

@Service
public class RemoveTeamMemberUseCase {

	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamAccessGuard teamAccessGuard;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public RemoveTeamMemberUseCase(
			TeamMembershipRepository teamMembershipRepository,
			TeamAccessGuard teamAccessGuard,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.teamAccessGuard = Objects.requireNonNull(teamAccessGuard);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void execute(AccountId actorAccountId, TeamId teamId, TeamMembershipId membershipId) {
		Team team = teamAccessGuard.requireActiveTeam(actorAccountId, teamId);
		TeamAccessGuard.ActorCapability actor = teamAccessGuard.requireManageRoster(actorAccountId, team);
		TeamMembership membership = teamMembershipRepository.findById(membershipId)
				.orElseThrow(TeamNotFoundException::new);
		if (!membership.teamId().equals(teamId) || !membership.isActive()) {
			throw new TeamNotFoundException();
		}
		if (membership.accountId().equals(actorAccountId)) {
			throw new MembershipConflictException("CANNOT_REMOVE_SELF", "Use leave to remove your own membership");
		}
		if (!InvitationAuthority.canRemoveTeamMember(actor.role(), actor.orgScoped(), membership.role())) {
			throw new TeamNotFoundException();
		}
		membership.remove(clock);
		teamMembershipRepository.save(membership);
		auditPort.membershipRemoved(
				membership.id().value(),
				team.organizationId(),
				team.id(),
				actorAccountId,
				membership.accountId());
	}

}
