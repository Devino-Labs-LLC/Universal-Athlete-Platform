package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;

@Service
public class LeaveTeamUseCase {

	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamRepository teamRepository;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public LeaveTeamUseCase(
			TeamMembershipRepository teamMembershipRepository,
			TeamRepository teamRepository,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void execute(AccountId accountId, TeamId teamId) {
		Team team = teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);
		TeamMembership membership = teamMembershipRepository.findActiveByTeamIdAndAccountId(teamId, accountId)
				.orElseThrow(TeamNotFoundException::new);
		membership.leave(clock);
		teamMembershipRepository.save(membership);
		auditPort.membershipLeft(membership.id().value(), team.organizationId(), team.id(), accountId);
	}

}
