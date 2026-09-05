package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamStatus;

@Service
public class UpdateTeamUseCase {

	private final TeamRepository teamRepository;
	private final OrganizationAccessGuard accessGuard;
	private final Clock clock;

	public UpdateTeamUseCase(TeamRepository teamRepository, OrganizationAccessGuard accessGuard, Clock clock) {
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TeamResult execute(AccountId accountId, TeamId teamId, String name, Long expectedVersion) {
		Team team = teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);
		requireManageAccessOrTeamNotFound(accountId, team);
		if (team.status() == TeamStatus.ARCHIVED) {
			throw new TeamArchivedException();
		}
		if (expectedVersion != null && team.version() != expectedVersion) {
			throw new ObjectOptimisticLockingFailureException(Team.class.getName(), teamId.value());
		}
		String normalizedName = name == null ? "" : name.trim();
		if (!team.name().equals(normalizedName)
				&& teamRepository.existsByOrganizationIdAndName(team.organizationId(), normalizedName)) {
			throw new IllegalArgumentException("A team with this name already exists in the organization");
		}
		try {
			team.rename(name, clock);
		}
		catch (IllegalStateException ex) {
			throw new TeamArchivedException();
		}
		return TeamResult.from(teamRepository.save(team));
	}

	private void requireManageAccessOrTeamNotFound(AccountId accountId, Team team) {
		try {
			accessGuard.requireManageAccess(accountId, team.organizationId());
		}
		catch (OrganizationNotFoundException ex) {
			throw new TeamNotFoundException();
		}
	}

}
