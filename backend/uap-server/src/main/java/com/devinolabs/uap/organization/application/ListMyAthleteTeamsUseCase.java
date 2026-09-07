package com.devinolabs.uap.organization.application;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationStatus;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamStatus;

/**
 * Lists ACTIVE athlete team memberships for the authenticated account (consent team picker).
 * Read-only; no hidden writes.
 */
@Service
public class ListMyAthleteTeamsUseCase {

	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamRepository teamRepository;
	private final OrganizationRepository organizationRepository;

	public ListMyAthleteTeamsUseCase(
			TeamMembershipRepository teamMembershipRepository,
			TeamRepository teamRepository,
			OrganizationRepository organizationRepository) {
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
	}

	@Transactional(readOnly = true)
	public List<MyAthleteTeamResult> execute(AccountId accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		return teamMembershipRepository.findAllActiveByAccountId(accountId).stream()
				.map(this::toResult)
				.flatMap(Optional::stream)
				.sorted(Comparator.comparing(MyAthleteTeamResult::teamName)
						.thenComparing(MyAthleteTeamResult::organizationName))
				.toList();
	}

	private Optional<MyAthleteTeamResult> toResult(TeamMembership membership) {
		if (membership.role() != OrganizationMembershipRole.ATHLETE || membership.athleteId() == null) {
			return Optional.empty();
		}
		Optional<Team> teamOpt = teamRepository.findById(membership.teamId());
		if (teamOpt.isEmpty()) {
			return Optional.empty();
		}
		Team team = teamOpt.get();
		if (team.status() != TeamStatus.ACTIVE) {
			return Optional.empty();
		}
		Optional<Organization> orgOpt = organizationRepository.findById(team.organizationId());
		if (orgOpt.isEmpty()) {
			return Optional.empty();
		}
		Organization organization = orgOpt.get();
		if (organization.status() != OrganizationStatus.ACTIVE) {
			return Optional.empty();
		}
		return Optional.of(new MyAthleteTeamResult(
				membership.id().value(),
				team.id().value(),
				team.name(),
				organization.id().value(),
				organization.name(),
				membership.athleteId()));
	}

}
