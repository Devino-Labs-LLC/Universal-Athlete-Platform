package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.TeamId;

@Service
public class ListTeamMembershipsUseCase {

	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamAccessGuard teamAccessGuard;

	public ListTeamMembershipsUseCase(
			TeamMembershipRepository teamMembershipRepository,
			TeamAccessGuard teamAccessGuard) {
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.teamAccessGuard = Objects.requireNonNull(teamAccessGuard);
	}

	@Transactional(readOnly = true)
	public List<TeamMembershipResult> execute(AccountId accountId, TeamId teamId) {
		teamAccessGuard.requireVisibleTeam(accountId, teamId);
		return teamMembershipRepository.findAllByTeamId(teamId).stream()
				.map(TeamMembershipResult::from)
				.toList();
	}

}
