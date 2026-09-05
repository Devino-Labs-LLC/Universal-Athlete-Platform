package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamStatus;

@Service
public class ArchiveTeamUseCase {

	private final TeamRepository teamRepository;
	private final OrganizationAccessGuard accessGuard;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public ArchiveTeamUseCase(
			TeamRepository teamRepository,
			OrganizationAccessGuard accessGuard,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void execute(AccountId accountId, TeamId teamId) {
		Team team = teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);
		try {
			accessGuard.requireManageAccess(accountId, team.organizationId());
		}
		catch (OrganizationNotFoundException ex) {
			throw new TeamNotFoundException();
		}
		if (team.status() == TeamStatus.ARCHIVED) {
			throw new TeamArchivedException();
		}
		try {
			team.archive(clock);
		}
		catch (IllegalStateException ex) {
			throw new TeamArchivedException();
		}
		teamRepository.save(team);
		auditPort.logTeamArchived(team.id().value(), team.organizationId(), accountId);
	}

}
