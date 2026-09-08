package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.training.domain.TrainingAssignment;
import com.devinolabs.uap.training.domain.TrainingAssignmentStatus;

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
				new ConsentHistoryPort(List.of(new ConsentHistory(
						TEAM_ID,
						Instant.parse("2026-09-02T12:00:00Z"),
						Instant.parse("2026-09-04T12:00:00Z")))),
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

	@Test
	void projectsRemovalAssignmentResponsesAndPaginatesWithoutRawPayloads() {
		UUID unknownTeam = UUID.fromString("10000000-0000-0000-0000-000000000099");
		GetAthleteTransparencyUseCase useCase = new GetAthleteTransparencyUseCase(
				athletePort(),
				new HistoryPort(List.of(
						new AthleteMembershipHistory(
								TEAM_ID,
								"Varsity",
								ORG_ID,
								"Devino",
								"REMOVED",
								Instant.parse("2026-09-01T12:00:00Z"),
								Instant.parse("2026-09-05T12:00:00Z")),
						new AthleteMembershipHistory(
								unknownTeam,
								"JV",
								ORG_ID,
								"Devino",
								"ACTIVE",
								Instant.parse("2026-08-01T12:00:00Z"),
								Instant.parse("2026-08-01T12:00:00Z")))),
				new ConsentHistoryPort(List.of(
						new ConsentHistory(unknownTeam, Instant.parse("2026-09-02T12:00:00Z"), null))),
				new AssignmentPort(List.of(
						assignment(TrainingAssignmentStatus.DECLINED, Instant.parse("2026-09-06T12:00:00Z")),
						assignment(TrainingAssignmentStatus.UNABLE, Instant.parse("2026-09-07T12:00:00Z")),
						assignment(unknownTeam, TrainingAssignmentStatus.ASSIGNED, null),
						assignment(TrainingAssignmentStatus.DECLINED, null))));

		GetAthleteTransparencyUseCase.TransparencyPage first = useCase.execute(ACCOUNT_ID, -1, 0);
		assertThat(first.page()).isZero();
		assertThat(first.size()).isEqualTo(GetAthleteTransparencyUseCase.DEFAULT_PAGE_SIZE);
		assertThat(first.events()).extracting(GetAthleteTransparencyUseCase.TransparencyEvent::type)
				.contains(
						"TEAM_REMOVED",
						"TEAM_JOINED",
						"ASSIGNMENT_CREATED",
						"ASSIGNMENT_DECLINED",
						"ASSIGNMENT_UNABLE",
						"CONSENT_GRANTED")
				.doesNotContain("CONSENT_REVOKED");
		assertThat(first.events()).filteredOn(event -> "ASSIGNMENT_DECLINED".equals(event.type())).hasSize(1);
		assertThat(first.events()).allSatisfy(event -> {
			assertThat(event.description()).doesNotContain("idempotency");
			assertThat(event.organizationName()).isIn("Devino", null);
		});

		GetAthleteTransparencyUseCase.TransparencyPage page = useCase.execute(ACCOUNT_ID, 1, 1);
		assertThat(page.events()).hasSize(1);
		assertThat(page.hasMore()).isTrue();
		assertThat(useCase.execute(ACCOUNT_ID, 50, GetAthleteTransparencyUseCase.MAX_PAGE_SIZE).events()).isEmpty();
	}

	@Test
	void rejectsMissingAccount() {
		GetAthleteTransparencyUseCase useCase = new GetAthleteTransparencyUseCase(
				athletePort(),
				historyPort(),
				new ConsentHistoryPort(),
				new AssignmentPort());
		org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> useCase.execute(null, 0, 20));
	}

	private static AthleteContextPort athletePort() {
		return new AthleteContextPort() {
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
		};
	}

	private static TrainingAssignment assignment(TrainingAssignmentStatus status, Instant respondedAt) {
		return assignment(TEAM_ID, status, respondedAt);
	}

	private static TrainingAssignment assignment(UUID teamId, TrainingAssignmentStatus status, Instant respondedAt) {
		return TrainingAssignment.rehydrate(
				UUID.randomUUID(),
				ATHLETE_ID,
				teamId,
				ORG_ID,
				UUID.fromString("60000000-0000-0000-0000-000000000001"),
				ACCOUNT_ID,
				"COACH",
				"Tempo",
				null,
				LocalDate.parse("2026-09-08"),
				status,
				null,
				respondedAt,
				"idempotency-key",
				Instant.parse("2026-09-05T12:00:00Z"),
				Instant.parse("2026-09-05T12:00:00Z"),
				0L);
	}

	private static OrganizationMembershipPort historyPort() {
		return new HistoryPort(List.of(new AthleteMembershipHistory(
				TEAM_ID,
				"Varsity",
				ORG_ID,
				"Devino",
				"LEFT",
				Instant.parse("2026-09-01T12:00:00Z"),
				Instant.parse("2026-09-03T12:00:00Z"))));
	}

	private static final class HistoryPort implements OrganizationMembershipPort {
		private final List<AthleteMembershipHistory> history;

		private HistoryPort(List<AthleteMembershipHistory> history) {
			this.history = history;
		}

		@Override
		public List<AthleteMembershipHistory> listAthleteMembershipHistory(UUID accountId) {
			return history;
		}

		@Override
		public Optional<TeamLifecycleRef> findTeamLifecycle(UUID teamId) {
			if (TEAM_ID.equals(teamId)) {
				return Optional.of(new TeamLifecycleRef(TEAM_ID, ORG_ID, "Varsity", "Devino", "ACTIVE", "ACTIVE"));
			}
			return Optional.empty();
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
	}

	private static final class ConsentHistoryPort implements ConsentGrantsPort {
		private final List<ConsentHistory> history;

		private ConsentHistoryPort() {
			this(List.of());
		}

		private ConsentHistoryPort(List<ConsentHistory> history) {
			this.history = history;
		}

		@Override
		public List<ConsentHistory> listConsentHistory(UUID athleteId) {
			assertThat(athleteId).isEqualTo(ATHLETE_ID);
			return history;
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
		private final List<TrainingAssignment> assignments;

		private AssignmentPort() {
			this(List.of());
		}

		private AssignmentPort(List<TrainingAssignment> assignments) {
			this.assignments = assignments;
		}

		@Override
		public List<TrainingAssignment> findByAthlete(UUID athleteId, int limit) {
			assertThat(limit).isEqualTo(GetAthleteTransparencyUseCase.SOURCE_LIMIT);
			return assignments;
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
