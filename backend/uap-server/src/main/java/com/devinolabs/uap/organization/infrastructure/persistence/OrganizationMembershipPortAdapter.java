package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.application.OrganizationMembershipRepository;
import com.devinolabs.uap.organization.application.OrganizationRepository;
import com.devinolabs.uap.organization.application.TeamMembershipRepository;
import com.devinolabs.uap.organization.application.TeamRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;

@Component
class OrganizationMembershipPortAdapter implements OrganizationMembershipPort {

	private final OrganizationMembershipRepository membershipRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamRepository teamRepository;
	private final OrganizationRepository organizationRepository;

	OrganizationMembershipPortAdapter(
			OrganizationMembershipRepository membershipRepository,
			TeamMembershipRepository teamMembershipRepository,
			TeamRepository teamRepository,
			OrganizationRepository organizationRepository) {
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
	}

	@Override
	public boolean hasActiveOrganizationMembership(UUID accountId, UUID organizationId) {
		return membershipRepository.existsActiveMembership(
				AccountId.of(accountId),
				OrganizationId.of(organizationId));
	}

	@Override
	public boolean canManageOrganization(UUID accountId, UUID organizationId) {
		return membershipRepository.existsActiveOwner(
				AccountId.of(accountId),
				OrganizationId.of(organizationId));
	}

	@Override
	public Optional<TeamMembershipRef> findTeamMembership(UUID membershipId) {
		return teamMembershipRepository.findById(TeamMembershipId.of(membershipId))
				.flatMap(this::toRef);
	}

	@Override
	public Optional<TeamMembershipRef> findActiveAthleteTeamMembership(UUID accountId, UUID teamId) {
		return teamMembershipRepository.findActiveByTeamIdAndAccountId(TeamId.of(teamId), AccountId.of(accountId))
				.filter(membership -> membership.role() == OrganizationMembershipRole.ATHLETE)
				.filter(membership -> membership.athleteId() != null)
				.filter(TeamMembership::isActive)
				.flatMap(this::toRef);
	}

	@Override
	public Optional<TeamLifecycleRef> findTeamLifecycle(UUID teamId) {
		Optional<Team> team = teamRepository.findById(TeamId.of(teamId));
		if (team.isEmpty()) {
			return Optional.empty();
		}
		Optional<Organization> organization = organizationRepository.findById(team.get().organizationId());
		if (organization.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new TeamLifecycleRef(
				team.get().id().value(),
				organization.get().id().value(),
				team.get().name(),
				organization.get().name(),
				team.get().status().name(),
				organization.get().status().name()));
	}

	private Optional<TeamMembershipRef> toRef(TeamMembership membership) {
		Optional<Team> team = teamRepository.findById(membership.teamId());
		if (team.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new TeamMembershipRef(
				membership.id().value(),
				membership.teamId().value(),
				team.get().organizationId().value(),
				membership.accountId().value(),
				membership.athleteId(),
				membership.role().name(),
				membership.status().name()));
	}

}
