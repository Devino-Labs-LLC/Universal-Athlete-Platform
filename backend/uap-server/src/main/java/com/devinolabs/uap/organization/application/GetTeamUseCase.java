package com.devinolabs.uap.organization.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;

@Service
public class GetTeamUseCase {

	private final TeamRepository teamRepository;
	private final OrganizationAccessGuard accessGuard;

	public GetTeamUseCase(TeamRepository teamRepository, OrganizationAccessGuard accessGuard) {
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
	}

	@Transactional(readOnly = true)
	public TeamResult execute(AccountId accountId, TeamId teamId) {
		Team team = teamRepository.findById(teamId).orElseThrow(TeamNotFoundException::new);
		try {
			accessGuard.requireActiveMember(accountId, team.organizationId());
		}
		catch (OrganizationNotFoundException ex) {
			throw new TeamNotFoundException();
		}
		return TeamResult.from(team);
	}

}
