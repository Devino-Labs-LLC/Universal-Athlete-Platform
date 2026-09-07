package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;

@Service
public class ListTeamInvitationsUseCase {

	private final InvitationRepository invitationRepository;
	private final TeamAccessGuard teamAccessGuard;

	public ListTeamInvitationsUseCase(InvitationRepository invitationRepository, TeamAccessGuard teamAccessGuard) {
		this.invitationRepository = Objects.requireNonNull(invitationRepository);
		this.teamAccessGuard = Objects.requireNonNull(teamAccessGuard);
	}

	@Transactional(readOnly = true)
	public List<InvitationResult> execute(AccountId accountId, TeamId teamId) {
		Team team = teamAccessGuard.requireVisibleTeam(accountId, teamId);
		teamAccessGuard.requireListInvitationsCapability(accountId, team);
		return invitationRepository.findAllByTeamId(teamId).stream()
				.map(InvitationResult::from)
				.toList();
	}

}
