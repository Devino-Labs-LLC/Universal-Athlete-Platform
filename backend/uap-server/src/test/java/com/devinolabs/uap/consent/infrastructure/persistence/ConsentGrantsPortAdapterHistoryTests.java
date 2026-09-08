package com.devinolabs.uap.consent.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.consent.api.ConsentGrantsPort.ConsentHistory;
import com.devinolabs.uap.consent.application.ConsentEffectiveAccessService;
import com.devinolabs.uap.consent.application.ConsentGrantRepository;
import com.devinolabs.uap.consent.domain.ConsentGrant;
import com.devinolabs.uap.consent.domain.ConsentGrantId;
import com.devinolabs.uap.consent.domain.ConsentGrantStatus;
import com.devinolabs.uap.consent.domain.ConsentScope;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamMembershipRef;

class ConsentGrantsPortAdapterHistoryTests {

	private static final UUID ATHLETE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID TEAM_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

	@Test
	void listsConsentHistoryFromAuthoritativeGrantsAndTreatsNullAthleteAsEmpty() {
		Instant grantedAt = Instant.parse("2026-09-02T12:00:00Z");
		Instant revokedAt = Instant.parse("2026-09-04T12:00:00Z");
		ConsentGrant grant = ConsentGrant.rehydrate(
				ConsentGrantId.of(UUID.fromString("70000000-0000-0000-0000-000000000001")),
				ATHLETE_ID,
				TEAM_ID,
				UUID.fromString("40000000-0000-0000-0000-000000000001"),
				UUID.fromString("60000000-0000-0000-0000-000000000001"),
				Set.of(ConsentScope.AVAILABILITY),
				ConsentGrantStatus.REVOKED,
				grantedAt,
				revokedAt,
				revokedAt,
				1L);
		ConsentGrantsPortAdapter adapter = new ConsentGrantsPortAdapter(new ConsentEffectiveAccessService(
				new ConsentGrantRepository() {
					@Override
					public ConsentGrant save(ConsentGrant grant) {
						throw new UnsupportedOperationException();
					}

					@Override
					public Optional<ConsentGrant> findById(ConsentGrantId id) {
						throw new UnsupportedOperationException();
					}

					@Override
					public List<ConsentGrant> findActiveByAthleteIdAndTeamId(UUID athleteId, UUID teamId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public List<ConsentGrant> findActiveByTeamId(UUID teamId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public Optional<ConsentGrant> findActiveByTeamMembershipId(UUID teamMembershipId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public List<ConsentGrant> findAllByAthleteId(UUID athleteId) {
						assertThat(athleteId).isEqualTo(ATHLETE_ID);
						return List.of(grant);
					}

					@Override
					public List<ConsentGrant> findAllByAthleteIdAndStatus(UUID athleteId, ConsentGrantStatus status) {
						throw new UnsupportedOperationException();
					}
				},
				new OrganizationMembershipPort() {
					@Override
					public List<OrganizationMembershipPort.AthleteMembershipHistory> listAthleteMembershipHistory(UUID accountId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public Optional<OrganizationMembershipPort.TeamLifecycleRef> findTeamLifecycle(UUID teamId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public boolean hasActiveOrganizationMembership(UUID accountId, UUID organizationId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public boolean canManageOrganization(UUID accountId, UUID organizationId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public Optional<TeamMembershipRef> findTeamMembership(UUID membershipId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public Optional<TeamMembershipRef> findActiveAthleteTeamMembership(UUID accountId, UUID teamId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public Optional<TeamMembershipRef> findActiveAthleteMembershipByAthleteIdAndTeamId(
							UUID athleteId,
							UUID teamId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public Optional<TeamMembershipRef> findActiveTeamMembership(UUID accountId, UUID teamId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public boolean canViewTeam(UUID accountId, UUID teamId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public boolean canViewTeamReadinessAggregate(UUID accountId, UUID teamId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public List<TeamMembershipRef> listActiveAthleteMemberships(UUID teamId) {
						throw new UnsupportedOperationException();
					}
				}));

		assertThat(adapter.listConsentHistory(null)).isEmpty();
		List<ConsentHistory> history = adapter.listConsentHistory(ATHLETE_ID);
		assertThat(history).singleElement().satisfies(row -> {
			assertThat(row.teamId()).isEqualTo(TEAM_ID);
			assertThat(row.grantedAt()).isEqualTo(grantedAt);
			assertThat(row.revokedAt()).isEqualTo(revokedAt);
		});
	}
}
