package com.devinolabs.uap.training.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.consent.api.ConsentGrantsPort;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.AthleteMembershipHistory;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamLifecycleRef;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamMembershipRef;
import com.devinolabs.uap.training.application.GetAthleteTransparencyUseCase;
import com.devinolabs.uap.training.application.TrainingAssignmentRepository;
import com.devinolabs.uap.training.infrastructure.web.AthleteTransparencyController.AthleteTransparencyResponse;

class AthleteTransparencyControllerTests {

	private static final UUID ACCOUNT_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID ATHLETE_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID TEAM_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID ORG_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

	@Test
	void mapsAllowListedFieldsAndRequiresAuthenticatedAccount() {
		AthleteTransparencyController controller = new AthleteTransparencyController(useCase());

		AthleteTransparencyResponse response = controller.transparency(
				0,
				20,
				new UsernamePasswordAuthenticationToken(new AccountPrincipal(AccountId.of(ACCOUNT_ID)), null));

		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.hasMore()).isFalse();
		assertThat(response.events()).singleElement().satisfies(event -> {
			assertThat(event.type()).isEqualTo("TEAM_JOINED");
			assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-09-01T12:00:00Z"));
			assertThat(event.organizationName()).isEqualTo("Devino");
			assertThat(event.teamName()).isEqualTo("Varsity");
			assertThat(event.description()).isEqualTo("You joined Varsity.");
		});

		assertThatThrownBy(() -> controller.transparency(0, 20, null))
				.isInstanceOf(IllegalStateException.class);
		Authentication stranger = new UsernamePasswordAuthenticationToken("not-an-account", null);
		assertThatThrownBy(() -> controller.transparency(0, 20, stranger))
				.isInstanceOf(IllegalStateException.class);
	}

	private static GetAthleteTransparencyUseCase useCase() {
		return new GetAthleteTransparencyUseCase(
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
				new OrganizationMembershipPort() {
					@Override
					public List<AthleteMembershipHistory> listAthleteMembershipHistory(UUID accountId) {
						return List.of(new AthleteMembershipHistory(
								TEAM_ID,
								"Varsity",
								ORG_ID,
								"Devino",
								"ACTIVE",
								Instant.parse("2026-09-01T12:00:00Z"),
								Instant.parse("2026-09-01T12:00:00Z")));
					}

					@Override
					public Optional<TeamLifecycleRef> findTeamLifecycle(UUID teamId) {
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
				},
				new ConsentGrantsPort() {
					@Override
					public List<ConsentGrantsPort.ConsentHistory> listConsentHistory(UUID athleteId) {
						return List.of();
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
				},
				new TrainingAssignmentRepository() {
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
				});
	}
}
