package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.consent.api.ConsentGrantsPort;
import com.devinolabs.uap.consent.api.ConsentGrantsPort.ConsentHistory;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.AthleteMembershipHistory;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamLifecycleRef;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamMembershipRef;

class GetAthleteTransparencyUseCaseTests {

	private static final UUID ACCOUNT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID ATHLETE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID TEAM_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID ORG_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

	@Test
	void projectsOwnMembershipSharingAndAssignmentEventsWithoutIdentifiers() {
		GetAthleteTransparencyUseCase useCase = new GetAthleteTransparencyUseCase(
				new AthleteContextPort() {
					@Override
					public AthleteRef requireMutableAthleteForUpdate(UUID accountId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public AthleteRef requireAthlete(UUID accountId) {
						return new AthleteRef(ATHLETE_ID);
					}

					@Override
					public void assertOptionalSportOwned(UUID athleteId, UUID sportId) {
						throw new UnsupportedOperationException();
					}

					@Override
					public void assertOptionalGoalOwned(UUID athleteId, UUID goalId) {
						throw new UnsupportedOperationException();
					}
				},
				historyPort(),
				new ConsentHistoryPort(),
				new AssignmentPort());

		GetAthleteTransparencyUseCase.TransparencyPage page = useCase.execute(ACCOUNT_ID, 0, 20);

		assertThat(page.events()).extracting(GetAthleteTransparencyUseCase.TransparencyEvent::type)
				.contains("TEAM_JOINED", "TEAM_LEFT", "CONSENT_GRANTED", "CONSENT_REVOKED");
		assertThat(page.events()).allSatisfy(event -> {
			assertThat(event.description()).doesNotContain(ATHLETE_ID.toString());
			assertThat(event.description()).doesNotContain("token");
		});
		assertThat(page.hasMore()).isFalse();
	}

	private static OrganizationMembershipPort historyPort() {
		return new OrganizationMembershipPort() {
			@Override
			public List<AthleteMembershipHistory> listAthleteMembershipHistory(UUID accountId) {
				return List.of(new AthleteMembershipHistory(
						TEAM_ID,
						"Varsity",
						ORG_ID,
						"Devino",
						"LEFT",
						Instant.parse("2026-09-01T12:00:00Z"),
						Instant.parse("2026-09-03T12:00:00Z")));
			}

			@Override
			public Optional<TeamLifecycleRef> findTeamLifecycle(UUID teamId) {
				return Optional.of(new TeamLifecycleRef(TEAM_ID, ORG_ID, "Varsity", "Devino", "ACTIVE", "ACTIVE"));
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
			public Optional<TeamMembershipRef> findActiveAthleteMembershipByAthleteIdAndTeamId(UUID athleteId, UUID teamId) {
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
		};
	}

	private static final class ConsentHistoryPort implements ConsentGrantsPort {
		@Override
		public List<ConsentHistory> listConsentHistory(UUID athleteId) {
			assertThat(athleteId).isEqualTo(ATHLETE_ID);
			return List.of(new ConsentHistory(
					TEAM_ID,
					Instant.parse("2026-09-02T12:00:00Z"),
					Instant.parse("2026-09-04T12:00:00Z")));
		}

		@Override
		public boolean hasEffectiveScope(UUID athleteId, UUID teamId, String scope) {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.util.Set<String> effectiveScopes(UUID athleteId, UUID teamId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public java.util.Map<UUID, java.util.Set<String>> effectiveScopesForCurrentMemberships(
				UUID teamId,
				java.util.Map<UUID, UUID> currentMembershipIdToAthleteId) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class AssignmentPort implements TrainingAssignmentRepository {
		@Override
		public List<com.devinolabs.uap.training.domain.TrainingAssignment> findByAthlete(UUID athleteId, int limit) {
			return List.of();
		}

		@Override
		public com.devinolabs.uap.training.domain.TrainingAssignment save(
				com.devinolabs.uap.training.domain.TrainingAssignment assignment) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<com.devinolabs.uap.training.domain.TrainingAssignment> findById(UUID id) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<com.devinolabs.uap.training.domain.TrainingAssignment> findByActorIdempotency(
				UUID assignedByAccountId,
				UUID teamId,
				UUID athleteId,
				String idempotencyKey) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<com.devinolabs.uap.training.domain.TrainingAssignment> findByTeamAndAthlete(
				UUID teamId,
				UUID athleteId,
				int limit) {
			throw new UnsupportedOperationException();
		}
	}
}
