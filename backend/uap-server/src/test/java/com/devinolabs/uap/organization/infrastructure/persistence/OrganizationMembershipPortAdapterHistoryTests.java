package com.devinolabs.uap.organization.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.organization.api.OrganizationMembershipPort.AthleteMembershipHistory;
import com.devinolabs.uap.organization.application.OrganizationMembershipRepository;
import com.devinolabs.uap.organization.application.OrganizationRepository;
import com.devinolabs.uap.organization.application.TeamMembershipRepository;
import com.devinolabs.uap.organization.application.TeamRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;
import com.devinolabs.uap.organization.domain.OrganizationStatus;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;
import com.devinolabs.uap.organization.domain.TeamStatus;

class OrganizationMembershipPortAdapterHistoryTests {

	private static final UUID ACCOUNT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID TEAM_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID ORG_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
	private static final Instant JOINED = Instant.parse("2026-09-01T12:00:00Z");

	@Test
	void listsAthleteMembershipHistoryAndSkipsUnresolvableOrNonAthleteRows() {
		TeamId teamId = TeamId.of(TEAM_ID);
		OrganizationId organizationId = OrganizationId.of(ORG_ID);
		Team team = Team.rehydrate(teamId, organizationId, "Varsity", TeamStatus.ACTIVE, JOINED, JOINED, 0L);
		Organization organization = Organization.rehydrate(
				organizationId,
				"Devino",
				OrganizationStatus.ACTIVE,
				JOINED,
				JOINED,
				0L);
		TeamMembership athlete = TeamMembership.rehydrate(
				TeamMembershipId.of(UUID.fromString("60000000-0000-0000-0000-000000000001")),
				teamId,
				AccountId.of(ACCOUNT_ID),
				UUID.fromString("30000000-0000-0000-0000-000000000001"),
				OrganizationMembershipRole.ATHLETE,
				OrganizationMembershipStatus.REMOVED,
				JOINED,
				Instant.parse("2026-09-05T12:00:00Z"),
				1L);
		TeamMembership coach = TeamMembership.rehydrate(
				TeamMembershipId.of(UUID.fromString("60000000-0000-0000-0000-000000000002")),
				teamId,
				AccountId.of(ACCOUNT_ID),
				null,
				OrganizationMembershipRole.COACH,
				OrganizationMembershipStatus.ACTIVE,
				JOINED,
				JOINED,
				0L);
		TeamMembership missingTeam = TeamMembership.rehydrate(
				TeamMembershipId.of(UUID.fromString("60000000-0000-0000-0000-000000000003")),
				TeamId.of(UUID.fromString("10000000-0000-0000-0000-000000000099")),
				AccountId.of(ACCOUNT_ID),
				UUID.fromString("30000000-0000-0000-0000-000000000001"),
				OrganizationMembershipRole.ATHLETE,
				OrganizationMembershipStatus.ACTIVE,
				JOINED,
				JOINED,
				0L);

		OrganizationMembershipPortAdapter adapter = new OrganizationMembershipPortAdapter(
				unusedMemberships(),
				memberships(List.of(athlete, coach, missingTeam)),
				teams(teamId, team),
				organizations(organizationId, organization));

		assertThat(adapter.listAthleteMembershipHistory(null)).isEmpty();
		List<AthleteMembershipHistory> history = adapter.listAthleteMembershipHistory(ACCOUNT_ID);
		assertThat(history).singleElement().satisfies(row -> {
			assertThat(row.teamName()).isEqualTo("Varsity");
			assertThat(row.organizationName()).isEqualTo("Devino");
			assertThat(row.status()).isEqualTo("REMOVED");
			assertThat(row.joinedAt()).isEqualTo(JOINED);
		});
	}

	@Test
	void omitsHistoryWhenOrganizationCannotBeResolved() {
		TeamId teamId = TeamId.of(TEAM_ID);
		Team team = Team.rehydrate(
				teamId,
				OrganizationId.of(ORG_ID),
				"Varsity",
				TeamStatus.ACTIVE,
				JOINED,
				JOINED,
				0L);
		TeamMembership athlete = TeamMembership.rehydrate(
				TeamMembershipId.of(UUID.fromString("60000000-0000-0000-0000-000000000001")),
				teamId,
				AccountId.of(ACCOUNT_ID),
				UUID.fromString("30000000-0000-0000-0000-000000000001"),
				OrganizationMembershipRole.ATHLETE,
				OrganizationMembershipStatus.ACTIVE,
				JOINED,
				JOINED,
				0L);

		OrganizationMembershipPortAdapter adapter = new OrganizationMembershipPortAdapter(
				unusedMemberships(),
				memberships(List.of(athlete)),
				teams(teamId, team),
				organizations(null, null));

		assertThat(adapter.listAthleteMembershipHistory(ACCOUNT_ID)).isEmpty();
	}

	private static TeamMembershipRepository memberships(List<TeamMembership> rows) {
		return new TeamMembershipRepository() {
			@Override
			public TeamMembership save(TeamMembership membership) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<TeamMembership> findById(TeamMembershipId id) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<TeamMembership> findActiveByTeamIdAndAccountId(TeamId teamId, AccountId accountId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<TeamMembership> findActiveByTeamIdAndAthleteId(TeamId teamId, UUID athleteId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<TeamMembership> findAllByTeamId(TeamId teamId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<TeamMembership> findAllActiveByAccountId(AccountId accountId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<TeamMembership> findAllByAccountId(AccountId accountId) {
				return rows;
			}

			@Override
			public boolean existsActiveMembership(AccountId accountId, TeamId teamId) {
				throw new UnsupportedOperationException();
			}
		};
	}

	private static TeamRepository teams(TeamId teamId, Team team) {
		return new TeamRepository() {
			@Override
			public Team save(Team team) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<Team> findById(TeamId id) {
				return teamId.equals(id) ? Optional.of(team) : Optional.empty();
			}

			@Override
			public List<Team> findAllByOrganizationId(OrganizationId organizationId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean existsByOrganizationIdAndName(OrganizationId organizationId, String name) {
				throw new UnsupportedOperationException();
			}
		};
	}

	private static OrganizationRepository organizations(OrganizationId organizationId, Organization organization) {
		return new OrganizationRepository() {
			@Override
			public Organization save(Organization organization) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<Organization> findById(OrganizationId id) {
				if (organization == null || organizationId == null || !organizationId.equals(id)) {
					return Optional.empty();
				}
				return Optional.of(organization);
			}

			@Override
			public List<Organization> findAllById(Iterable<OrganizationId> ids) {
				throw new UnsupportedOperationException();
			}
		};
	}

	private static OrganizationMembershipRepository unusedMemberships() {
		return new OrganizationMembershipRepository() {
			@Override
			public com.devinolabs.uap.organization.domain.OrganizationMembership save(
					com.devinolabs.uap.organization.domain.OrganizationMembership membership) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<com.devinolabs.uap.organization.domain.OrganizationMembership> findById(
					com.devinolabs.uap.organization.domain.OrganizationMembershipId id) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<com.devinolabs.uap.organization.domain.OrganizationMembership> findActiveByOrganizationIdAndAccountId(
					OrganizationId organizationId,
					AccountId accountId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Optional<com.devinolabs.uap.organization.domain.OrganizationMembership> findActiveOwner(
					OrganizationId organizationId,
					AccountId accountId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<com.devinolabs.uap.organization.domain.OrganizationMembership> findAllActiveByAccountId(
					AccountId accountId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public List<com.devinolabs.uap.organization.domain.OrganizationMembership> findAllByOrganizationId(
					OrganizationId organizationId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public long countActiveOwners(OrganizationId organizationId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean existsActiveMembership(AccountId accountId, OrganizationId organizationId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public boolean existsActiveOwner(AccountId accountId, OrganizationId organizationId) {
				throw new UnsupportedOperationException();
			}
		};
	}
}
