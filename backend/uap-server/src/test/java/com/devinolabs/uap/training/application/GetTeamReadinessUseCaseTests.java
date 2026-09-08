package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.consent.api.ConsentGrantsPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamLifecycleRef;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamMembershipRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.AggregateStatus;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.CellPublication;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.CohortPublication;

class GetTeamReadinessUseCaseTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-07T15:00:00Z"), ZoneOffset.UTC);
	private static final LocalDate TODAY = LocalDate.of(2026, 9, 7);
	private static final UUID TEAM_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID COACH_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	@Test
	void emptyRosterDoesNotQueryReadinessRows() {
		ReadinessStub readiness = new ReadinessStub(List.of(), List.of());
		GetTeamReadinessUseCase useCase = new GetTeamReadinessUseCase(
				new MembershipStub(true, List.of()),
				new ConsentFillingStub(List.of()),
				readiness,
				CLOCK);

		TeamReadinessResult result = useCase.execute(COACH_ID, TEAM_ID, TODAY);

		assertThat(result.status()).isEqualTo(AggregateStatus.INSUFFICIENT_DATA);
		assertThat(result.includedCount()).isNull();
		assertThat(readiness.requestedAthletes).isEmpty();
		assertThat(readiness.dimensionQueries).isEqualTo(1);
	}

	@Test
	void unauthorizedViewerIsNotFound() {
		GetTeamReadinessUseCase useCase = new GetTeamReadinessUseCase(
				new MembershipStub(false, List.of()),
				new ConsentFillingStub(List.of()),
				new ReadinessStub(List.of(), List.of()),
				CLOCK);

		assertThatThrownBy(() -> useCase.execute(COACH_ID, TEAM_ID, TODAY))
				.isInstanceOf(TeamReadinessNotFoundException.class);
	}

	@Test
	void nullDateUsesClockAndAggregatesStoredReadinessOnly() {
		List<TeamMembershipRef> athletes = new ArrayList<>();
		List<CurrentReadinessSlice> slices = new ArrayList<>();
		List<StoredLimitingDimension> dimensions = new ArrayList<>();
		ReadinessBand[] bands = ReadinessBand.values();
		for (int index = 0; index < 20; index++) {
			UUID athleteId = UUID.nameUUIDFromBytes(("athlete-" + index).getBytes());
			UUID membershipId = UUID.nameUUIDFromBytes(("membership-" + index).getBytes());
			UUID assessmentId = UUID.nameUUIDFromBytes(("assessment-" + index).getBytes());
			athletes.add(membership(membershipId, athleteId));
			slices.add(new CurrentReadinessSlice(athleteId, assessmentId, bands[index % bands.length]));
			dimensions.add(new StoredLimitingDimension(athleteId, ReadinessDimensionType.FATIGUE));
		}
		UUID duplicatedAthlete = athletes.get(0).athleteId();
		slices.add(new CurrentReadinessSlice(
				duplicatedAthlete,
				UUID.nameUUIDFromBytes("duplicate-assessment".getBytes()),
				ReadinessBand.LOW));
		dimensions.add(new StoredLimitingDimension(duplicatedAthlete, ReadinessDimensionType.FATIGUE));
		dimensions.add(new StoredLimitingDimension(
				UUID.nameUUIDFromBytes("outsider".getBytes()),
				ReadinessDimensionType.STRESS));

		ReadinessStub readiness = new ReadinessStub(slices, dimensions);
		GetTeamReadinessUseCase useCase = new GetTeamReadinessUseCase(
				new MembershipStub(true, athletes),
				new ConsentFillingStub(athletes),
				readiness,
				CLOCK);

		TeamReadinessResult result = useCase.execute(COACH_ID, TEAM_ID, null);

		assertThat(result.date()).isEqualTo(TODAY);
		assertThat(readiness.requestedDate).isEqualTo(TODAY);
		assertThat(readiness.requestedAthletes).hasSize(20);
		assertThat(result.availability().status()).isEqualTo("UNSUPPORTED");
		assertThat(result.status()).isEqualTo(AggregateStatus.PUBLISHED);
		assertThat(result.cohort()).isEqualTo(CohortPublication.EXACT);
		assertThat(result.includedCount()).isEqualTo(20);
		assertThat(result.categoryDistribution().cells())
				.filteredOn(cell -> cell.category().equals("HIGH"))
				.singleElement()
				.satisfies(cell -> {
					assertThat(cell.publication()).isEqualTo(CellPublication.PUBLISHED);
					assertThat(cell.count()).isEqualTo(5);
				});
		assertThat(result.limitingDimensionDistribution().includedCount()).isEqualTo(20);
		assertThat(result.limitingDimensionDistribution().cells())
				.filteredOn(cell -> cell.dimension().equals("FATIGUE"))
				.singleElement()
				.satisfies(cell -> {
					assertThat(cell.publication()).isEqualTo(CellPublication.PUBLISHED);
					assertThat(cell.count()).isEqualTo(20);
				});
		assertThat(result.limitingDimensionDistribution().cells())
				.noneMatch(cell -> cell.dimension().equals("STRESS"));
	}

	private static TeamMembershipRef membership(UUID membershipId, UUID athleteId) {
		return new TeamMembershipRef(
				membershipId,
				TEAM_ID,
				UUID.fromString("30000000-0000-0000-0000-000000000001"),
				athleteId,
				athleteId,
				"ATHLETE",
				"ACTIVE");
	}

	private static final class MembershipStub implements OrganizationMembershipPort {
		private final boolean allowed;
		private final List<TeamMembershipRef> athletes;

		private MembershipStub(boolean allowed, List<TeamMembershipRef> athletes) {
			this.allowed = allowed;
			this.athletes = athletes;
		}

		@Override
		public boolean canViewTeamReadinessAggregate(UUID accountId, UUID teamId) {
			return allowed;
		}

		@Override
		public List<TeamMembershipRef> listActiveAthleteMemberships(UUID teamId) {
			return athletes;
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
		public Optional<TeamLifecycleRef> findTeamLifecycle(UUID teamId) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class ConsentFillingStub implements ConsentGrantsPort {
		private final List<TeamMembershipRef> athletes;

		private ConsentFillingStub(List<TeamMembershipRef> athletes) {
			this.athletes = athletes;
		}

		@Override
		public Map<UUID, Set<String>> effectiveScopesForCurrentMemberships(
				UUID teamId,
				Map<UUID, UUID> currentMembershipIdToAthleteId) {
			return athletes.stream()
					.collect(java.util.stream.Collectors.toUnmodifiableMap(
							TeamMembershipRef::athleteId,
							ignored -> Set.of("READINESS_CATEGORY", "LIMITING_DIMENSIONS")));
		}

		@Override
		public boolean hasEffectiveScope(UUID athleteId, UUID teamId, String scope) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> effectiveScopes(UUID athleteId, UUID teamId) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class ReadinessStub implements DailyReadinessAssessmentRepository {
		private final List<CurrentReadinessSlice> slices;
		private final List<StoredLimitingDimension> dimensions;
		private LocalDate requestedDate;
		private Collection<UUID> requestedAthletes = Set.of();
		private int dimensionQueries;

		private ReadinessStub(List<CurrentReadinessSlice> slices, List<StoredLimitingDimension> dimensions) {
			this.slices = slices;
			this.dimensions = dimensions;
		}

		@Override
		public List<CurrentReadinessSlice> findCurrentSlicesByAthleteIdsAndDate(
				Collection<UUID> athleteIds,
				LocalDate stateDate,
				ReadinessAlgorithmVersion algorithmVersion) {
			requestedDate = stateDate;
			requestedAthletes = new HashSet<>(athleteIds);
			assertThat(algorithmVersion).isEqualTo(ReadinessAlgorithmVersion.READINESS_V1);
			return slices.stream().filter(slice -> athleteIds.contains(slice.athleteId())).toList();
		}

		@Override
		public List<StoredLimitingDimension> findLimitingDimensionsByAssessmentIds(Collection<UUID> assessmentIds) {
			dimensionQueries++;
			assertThat(assessmentIds).isNotNull();
			return dimensions;
		}

		@Override
		public DailyReadinessAssessment saveNew(DailyReadinessAssessment assessment) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<DailyReadinessAssessment> findByIdAndAthleteId(
				DailyReadinessAssessmentId id,
				AthleteId athleteId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<DailyReadinessAssessment> findBySnapshotIdAndAlgorithmVersion(
				DailyAthleteStateSnapshotId snapshotId,
				ReadinessAlgorithmVersion algorithmVersion,
				AthleteId athleteId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<DailyReadinessAssessmentSummary> findSummaryBySnapshotIdAndAlgorithmVersion(
				DailyAthleteStateSnapshotId snapshotId,
				ReadinessAlgorithmVersion algorithmVersion,
				AthleteId athleteId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<ReadinessDimensionType> findLimitingDimensionsByAssessmentId(
				DailyReadinessAssessmentId assessmentId,
				AthleteId athleteId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<DailyReadinessAssessmentSummary> findHistory(
				AthleteId athleteId,
				LocalDate startDate,
				LocalDate endDate,
				boolean currentSnapshotOnly,
				ReadinessAlgorithmVersion algorithmVersion,
				int page,
				int size) {
			throw new UnsupportedOperationException();
		}

		@Override
		public long countHistory(
				AthleteId athleteId,
				LocalDate startDate,
				LocalDate endDate,
				boolean currentSnapshotOnly,
				ReadinessAlgorithmVersion algorithmVersion) {
			throw new UnsupportedOperationException();
		}
	}

}
