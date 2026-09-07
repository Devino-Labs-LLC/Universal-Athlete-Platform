package com.devinolabs.uap.organization.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.rosteridentity.AthleteRosterIdentityPort;
import com.devinolabs.uap.athlete.api.rosteridentity.AthleteRosterIdentityPort.AthleteRosterIdentity;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;
import com.devinolabs.uap.organization.domain.OrganizationStatus;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamStatus;

@Service
public class GetTeamRosterUseCase {

	private final TeamAccessGuard teamAccessGuard;
	private final TeamMembershipRepository teamMembershipRepository;
	private final OrganizationRepository organizationRepository;
	private final AthleteRosterIdentityPort athleteRosterIdentityPort;

	public GetTeamRosterUseCase(
			TeamAccessGuard teamAccessGuard,
			TeamMembershipRepository teamMembershipRepository,
			OrganizationRepository organizationRepository,
			AthleteRosterIdentityPort athleteRosterIdentityPort) {
		this.teamAccessGuard = Objects.requireNonNull(teamAccessGuard);
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.athleteRosterIdentityPort = Objects.requireNonNull(athleteRosterIdentityPort);
	}

	@Transactional(readOnly = true)
	public List<TeamRosterEntryResult> execute(AccountId accountId, TeamId teamId) {
		Team team = teamAccessGuard.requireVisibleTeam(accountId, teamId);
		if (team.status() != TeamStatus.ACTIVE) {
			throw new TeamNotFoundException();
		}
		Organization organization = organizationRepository.findById(team.organizationId())
				.orElseThrow(TeamNotFoundException::new);
		if (organization.status() != OrganizationStatus.ACTIVE) {
			throw new TeamNotFoundException();
		}

		List<TeamMembership> athleteMemberships = teamMembershipRepository.findAllByTeamId(teamId).stream()
				.filter(membership -> membership.status() == OrganizationMembershipStatus.ACTIVE)
				.filter(membership -> membership.role() == OrganizationMembershipRole.ATHLETE)
				.filter(membership -> membership.athleteId() != null)
				.toList();

		List<UUID> athleteIds = athleteMemberships.stream()
				.map(TeamMembership::athleteId)
				.distinct()
				.toList();
		Map<UUID, AthleteRosterIdentity> identities = athleteRosterIdentityPort.findByAthleteIds(athleteIds);

		return athleteMemberships.stream()
				.map(membership -> toEntry(membership, identities.get(membership.athleteId())))
				.filter(Objects::nonNull)
				.sorted(Comparator
						.comparing(TeamRosterEntryResult::displayName, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(TeamRosterEntryResult::athleteId))
				.toList();
	}

	private static TeamRosterEntryResult toEntry(TeamMembership membership, AthleteRosterIdentity identity) {
		if (identity == null) {
			return null;
		}
		return new TeamRosterEntryResult(
				membership.athleteId(),
				membership.id().value(),
				identity.displayName(),
				OrganizationMembershipRole.ATHLETE.name(),
				OrganizationMembershipStatus.ACTIVE.name());
	}

}
