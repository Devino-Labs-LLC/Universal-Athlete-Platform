package com.devinolabs.uap.organization.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.TeamId;

@Service
public class GetTeamUseCase {

	private final TeamAccessGuard teamAccessGuard;

	public GetTeamUseCase(TeamAccessGuard teamAccessGuard) {
		this.teamAccessGuard = Objects.requireNonNull(teamAccessGuard);
	}

	@Transactional(readOnly = true)
	public TeamResult execute(AccountId accountId, TeamId teamId) {
		return TeamResult.from(teamAccessGuard.requireVisibleTeam(accountId, teamId));
	}

}
