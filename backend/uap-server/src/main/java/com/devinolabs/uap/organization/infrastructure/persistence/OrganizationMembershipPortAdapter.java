package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.AthleteMembershipHistory;
import com.devinolabs.uap.organization.application.OrganizationMembershipRepository;
import com.devinolabs.uap.organization.application.OrganizationRepository;
import com.devinolabs.uap.organization.application.TeamMembershipRepository;
import com.devinolabs.uap.organization.application.TeamRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationStatus;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;
import com.devinolabs.uap.organization.domain.TeamStatus;

@Component
class OrganizationMembershipPortAdapter implements OrganizationMembershipPort {

	private static final Set<OrganizationMembershipRole> TEAM_READINESS_ROLES = Set.of(
			OrganizationMembershipRole.COACH,
			OrganizationMembershipRole.HEAD_COACH,
			OrganizationMembershipRole.TEAM_ADMIN);
	private static final Set<OrganizationMembershipRole> ORG_READINESS_ROLES = Set.of(
			OrganizationMembershipRole.ORG_ADMIN,
			OrganizationMembershipRole.ORG_OWNER);

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
	public Optional<TeamMembershipRef> findActiveAthleteMembershipByAthleteIdAndTeamId(UUID athleteId, UUID teamId) {
		if (athleteId == null || teamId == null) {
			return Optional.empty();
		}
		return teamMembershipRepository.findActiveByTeamIdAndAthleteId(TeamId.of(teamId), athleteId)
				.filter(membership -> membership.role() == OrganizationMembershipRole.ATHLETE)
				.filter(membership -> athleteId.equals(membership.athleteId()))
				.filter(TeamMembership::isActive)
				.flatMap(this::toRef);
	}

	@Override
	public Optional<TeamMembershipRef> findActiveTeamMembership(UUID accountId, UUID teamId) {
		if (accountId == null || teamId == null) {
			return Optional.empty();
		}
		return teamMembershipRepository.findActiveByTeamIdAndAccountId(TeamId.of(teamId), AccountId.of(accountId))
				.filter(TeamMembership::isActive)
				.flatMap(this::toRef);
	}

	@Override
	public boolean canViewTeam(UUID accountId, UUID teamId) {
		if (accountId == null || teamId == null) {
			return false;
		}
		Optional<Team> team = teamRepository.findById(TeamId.of(teamId));
		if (team.isEmpty() || team.get().status() != TeamStatus.ACTIVE) {
			return false;
		}
		Optional<Organization> organization = organizationRepository.findById(team.get().organizationId());
		if (organization.isEmpty() || organization.get().status() != OrganizationStatus.ACTIVE) {
			return false;
		}
		AccountId account = AccountId.of(accountId);
		if (teamMembershipRepository.existsActiveMembership(account, team.get().id())) {
			return true;
		}
		return membershipRepository.existsActiveMembership(account, team.get().organizationId());
	}

	@Override
	public boolean canViewTeamReadinessAggregate(UUID accountId, UUID teamId) {
		if (accountId == null || teamId == null) {
			return false;
		}
		Optional<Team> team = teamRepository.findById(TeamId.of(teamId));
		if (team.isEmpty() || team.get().status() != TeamStatus.ACTIVE) {
			return false;
		}
		Optional<Organization> organization = organizationRepository.findById(team.get().organizationId());
		if (organization.isEmpty() || organization.get().status() != OrganizationStatus.ACTIVE) {
			return false;
		}
		AccountId account = AccountId.of(accountId);
		Optional<TeamMembership> teamMembership = teamMembershipRepository
				.findActiveByTeamIdAndAccountId(team.get().id(), account);
		if (teamMembership.isPresent()
				&& teamMembership.get().isActive()
				&& TEAM_READINESS_ROLES.contains(teamMembership.get().role())) {
			return true;
		}
		return membershipRepository.findActiveByOrganizationIdAndAccountId(team.get().organizationId(), account)
				.filter(membership -> membership.isActive())
				.filter(membership -> ORG_READINESS_ROLES.contains(membership.role()))
				.isPresent();
	}

	@Override
	public List<TeamMembershipRef> listActiveAthleteMemberships(UUID teamId) {
		if (teamId == null) {
			return List.of();
		}
		return teamMembershipRepository.findAllByTeamId(TeamId.of(teamId)).stream()
				.filter(TeamMembership::isActive)
				.filter(membership -> membership.role() == OrganizationMembershipRole.ATHLETE)
				.filter(membership -> membership.athleteId() != null)
				.map(this::toRef)
				.flatMap(Optional::stream)
				.toList();
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

	@Override
	public List<AthleteMembershipHistory> listAthleteMembershipHistory(UUID accountId) {
		if (accountId == null) {
			return List.of();
		}
		return teamMembershipRepository.findAllByAccountId(AccountId.of(accountId)).stream()
				.filter(membership -> membership.role() == OrganizationMembershipRole.ATHLETE)
				.map(this::toHistory)
				.flatMap(Optional::stream)
				.toList();
	}

	private Optional<AthleteMembershipHistory> toHistory(TeamMembership membership) {
		Optional<Team> team = teamRepository.findById(membership.teamId());
		if (team.isEmpty()) {
			return Optional.empty();
		}
		Optional<Organization> organization = organizationRepository.findById(team.get().organizationId());
		if (organization.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new AthleteMembershipHistory(
				team.get().id().value(),
				team.get().name(),
				organization.get().id().value(),
				organization.get().name(),
				membership.status().name(),
				membership.createdAt(),
				membership.updatedAt()));
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
