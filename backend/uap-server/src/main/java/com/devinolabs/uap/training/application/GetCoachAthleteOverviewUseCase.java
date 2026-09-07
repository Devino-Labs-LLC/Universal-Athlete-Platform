package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.rosteridentity.AthleteRosterIdentityPort;
import com.devinolabs.uap.consent.api.ConsentGrantsPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamLifecycleRef;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamMembershipRef;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.CoachOverviewSection;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.DiscomfortData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.LimitingDimensionsData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.PerformanceHistoryData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.PerformanceHistoryEntry;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.RatingData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.ReadinessCategoryData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.ReadinessScoreData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.RecoveryCheckInData;
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.TrainingAdherenceData;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecord;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;

/**
 * Coach athlete overview: roster-safe base + independent consent-scoped projections.
 * Reads stored state only — never generates readiness/state/recommendations.
 */
@Service
public class GetCoachAthleteOverviewUseCase {

	private static final String SCOPE_AVAILABILITY = "AVAILABILITY";
	private static final String SCOPE_READINESS_CATEGORY = "READINESS_CATEGORY";
	private static final String SCOPE_READINESS_SCORE = "READINESS_SCORE";
	private static final String SCOPE_LIMITING_DIMENSIONS = "LIMITING_DIMENSIONS";
	private static final String SCOPE_RECOVERY = "RECOVERY_CHECK_IN_DETAIL";
	private static final String SCOPE_ADHERENCE = "TRAINING_ADHERENCE";
	private static final String SCOPE_PERFORMANCE = "PERFORMANCE_HISTORY";

	private static final int PERFORMANCE_HISTORY_LIMIT = 5;
	private static final int PERFORMANCE_HISTORY_WINDOW_DAYS = 90;

	private final OrganizationMembershipPort organizationMembershipPort;
	private final AthleteRosterIdentityPort athleteRosterIdentityPort;
	private final ConsentGrantsPort consentGrantsPort;
	private final DailyReadinessAssessmentRepository readinessRepository;
	private final DailyRecoveryCheckInRepository recoveryCheckInRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final AthleteExercisePersonalRecordRepository personalRecordRepository;
	private final Clock clock;

	public GetCoachAthleteOverviewUseCase(
			OrganizationMembershipPort organizationMembershipPort,
			AthleteRosterIdentityPort athleteRosterIdentityPort,
			ConsentGrantsPort consentGrantsPort,
			DailyReadinessAssessmentRepository readinessRepository,
			DailyRecoveryCheckInRepository recoveryCheckInRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			AthleteExercisePersonalRecordRepository personalRecordRepository,
			Clock clock) {
		this.organizationMembershipPort = Objects.requireNonNull(organizationMembershipPort);
		this.athleteRosterIdentityPort = Objects.requireNonNull(athleteRosterIdentityPort);
		this.consentGrantsPort = Objects.requireNonNull(consentGrantsPort);
		this.readinessRepository = Objects.requireNonNull(readinessRepository);
		this.recoveryCheckInRepository = Objects.requireNonNull(recoveryCheckInRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.personalRecordRepository = Objects.requireNonNull(personalRecordRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public CoachAthleteOverviewResult execute(UUID accountId, UUID teamId, UUID athleteId, LocalDate date) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(teamId, "teamId must not be null");
		Objects.requireNonNull(athleteId, "athleteId must not be null");

		if (!organizationMembershipPort.canViewTeam(accountId, teamId)) {
			throw new CoachAthleteOverviewNotFoundException();
		}

		TeamMembershipRef membership = organizationMembershipPort
				.findActiveAthleteMembershipByAthleteIdAndTeamId(athleteId, teamId)
				.orElseThrow(CoachAthleteOverviewNotFoundException::new);

		TeamLifecycleRef lifecycle = organizationMembershipPort.findTeamLifecycle(teamId)
				.orElseThrow(CoachAthleteOverviewNotFoundException::new);
		if (!"ACTIVE".equals(lifecycle.teamStatus()) || !"ACTIVE".equals(lifecycle.organizationStatus())) {
			throw new CoachAthleteOverviewNotFoundException();
		}

		LocalDate viewDate = date == null ? LocalDate.now(clock) : date;
		String displayName = athleteRosterIdentityPort.findByAthleteId(athleteId)
				.map(identity -> identity.displayName())
				.orElse("");

		Set<String> effectiveScopes = new TreeSet<>(consentGrantsPort.effectiveScopes(athleteId, teamId));
		Optional<DailyReadinessAssessmentSummary> readiness = loadStoredReadiness(athleteId, viewDate);

		return new CoachAthleteOverviewResult(
				teamId,
				lifecycle.organizationId(),
				athleteId,
				membership.membershipId(),
				displayName,
				membership.role(),
				viewDate,
				Set.copyOf(effectiveScopes),
				projectAvailability(effectiveScopes),
				projectReadinessCategory(effectiveScopes, readiness),
				projectReadinessScore(effectiveScopes, readiness),
				projectLimitingDimensions(effectiveScopes, readiness, athleteId),
				projectRecovery(effectiveScopes, athleteId, viewDate),
				projectAdherence(effectiveScopes, athleteId, viewDate),
				projectPerformanceHistory(effectiveScopes, athleteId));
	}

	private Optional<DailyReadinessAssessmentSummary> loadStoredReadiness(UUID athleteId, LocalDate viewDate) {
		List<DailyReadinessAssessmentSummary> history = readinessRepository.findHistory(
				AthleteId.of(athleteId),
				viewDate,
				viewDate,
				true,
				ReadinessAlgorithmVersion.READINESS_V1,
				0,
				1);
		if (history.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(history.getFirst());
	}

	private static CoachOverviewSection<Void> projectAvailability(Set<String> scopes) {
		if (!scopes.contains(SCOPE_AVAILABILITY)) {
			return CoachOverviewSection.notShared();
		}
		return CoachOverviewSection.noData();
	}

	private static CoachOverviewSection<ReadinessCategoryData> projectReadinessCategory(
			Set<String> scopes,
			Optional<DailyReadinessAssessmentSummary> readiness) {
		if (!scopes.contains(SCOPE_READINESS_CATEGORY)) {
			return CoachOverviewSection.notShared();
		}
		if (readiness.isEmpty()) {
			return CoachOverviewSection.noData();
		}
		DailyReadinessAssessmentSummary summary = readiness.get();
		return CoachOverviewSection.available(new ReadinessCategoryData(
				summary.readinessBand().name(),
				summary.dataSufficiency().name()));
	}

	private static CoachOverviewSection<ReadinessScoreData> projectReadinessScore(
			Set<String> scopes,
			Optional<DailyReadinessAssessmentSummary> readiness) {
		if (!scopes.contains(SCOPE_READINESS_SCORE)) {
			return CoachOverviewSection.notShared();
		}
		if (readiness.isEmpty()) {
			return CoachOverviewSection.noData();
		}
		DailyReadinessAssessmentSummary summary = readiness.get();
		return CoachOverviewSection.available(new ReadinessScoreData(
				summary.readinessScore(),
				summary.dataSufficiency().name(),
				summary.summaryReasonCode().name()));
	}

	private CoachOverviewSection<LimitingDimensionsData> projectLimitingDimensions(
			Set<String> scopes,
			Optional<DailyReadinessAssessmentSummary> readiness,
			UUID athleteId) {
		if (!scopes.contains(SCOPE_LIMITING_DIMENSIONS)) {
			return CoachOverviewSection.notShared();
		}
		if (readiness.isEmpty()) {
			return CoachOverviewSection.noData();
		}
		List<ReadinessDimensionType> limiting = readinessRepository.findLimitingDimensionsByAssessmentId(
				DailyReadinessAssessmentId.of(readiness.get().assessmentId()),
				AthleteId.of(athleteId));
		return CoachOverviewSection.available(new LimitingDimensionsData(
				limiting.stream().map(Enum::name).toList()));
	}

	private CoachOverviewSection<RecoveryCheckInData> projectRecovery(
			Set<String> scopes,
			UUID athleteId,
			LocalDate viewDate) {
		if (!scopes.contains(SCOPE_RECOVERY)) {
			return CoachOverviewSection.notShared();
		}
		Optional<DailyRecoveryCheckIn> checkIn = recoveryCheckInRepository
				.findByAthleteIdAndCheckInDate(AthleteId.of(athleteId), viewDate);
		if (checkIn.isEmpty()) {
			return CoachOverviewSection.noData();
		}
		DailyRecoveryCheckIn stored = checkIn.get();
		return CoachOverviewSection.available(new RecoveryCheckInData(
				stored.checkInDate(),
				stored.sleepQuality() == null
						? null
						: new RatingData(stored.sleepQuality().value(), stored.sleepQuality().label()),
				new RatingData(stored.mood().value(), stored.mood().label()),
				new RatingData(stored.fatigue().value(), stored.fatigue().label()),
				new RatingData(stored.muscleSoreness().value(), stored.muscleSoreness().label()),
				new RatingData(stored.stress().value(), stored.stress().label()),
				stored.notes(),
				stored.completeness().name(),
				stored.discomfortAreas().stream().map(GetCoachAthleteOverviewUseCase::toDiscomfort).toList()));
	}

	private static DiscomfortData toDiscomfort(BodyAreaDiscomfortObservation observation) {
		return new DiscomfortData(
				observation.bodyArea().name(),
				observation.side().name(),
				new RatingData(observation.intensity().value(), observation.intensity().label()),
				observation.notes(),
				observation.orderIndex());
	}

	private CoachOverviewSection<TrainingAdherenceData> projectAdherence(
			Set<String> scopes,
			UUID athleteId,
			LocalDate viewDate) {
		if (!scopes.contains(SCOPE_ADHERENCE)) {
			return CoachOverviewSection.notShared();
		}
		List<WorkoutOccurrence> occurrences = workoutOccurrenceRepository.findCalendarRange(
				AthleteId.of(athleteId),
				viewDate,
				viewDate,
				null,
				null);
		int scheduled = 0;
		int completed = 0;
		int skipped = 0;
		int inProgress = 0;
		int cancelled = 0;
		for (WorkoutOccurrence occurrence : occurrences) {
			switch (occurrence.status()) {
				case SCHEDULED -> scheduled++;
				case COMPLETED -> completed++;
				case SKIPPED -> skipped++;
				case IN_PROGRESS -> inProgress++;
				case CANCELLED -> cancelled++;
			}
		}
		if (occurrences.isEmpty()) {
			return CoachOverviewSection.noData();
		}
		return CoachOverviewSection.available(new TrainingAdherenceData(
				scheduled,
				completed,
				skipped,
				inProgress,
				cancelled));
	}

	private CoachOverviewSection<PerformanceHistoryData> projectPerformanceHistory(
			Set<String> scopes,
			UUID athleteId) {
		if (!scopes.contains(SCOPE_PERFORMANCE)) {
			return CoachOverviewSection.notShared();
		}
		Instant achievedFrom = Instant.now(clock).minus(PERFORMANCE_HISTORY_WINDOW_DAYS, ChronoUnit.DAYS);
		List<AthleteExercisePersonalRecord> records = personalRecordRepository.findRecentByAthleteId(
				AthleteId.of(athleteId),
				achievedFrom,
				PERFORMANCE_HISTORY_LIMIT);
		if (records.isEmpty()) {
			return CoachOverviewSection.noData();
		}
		List<PerformanceHistoryEntry> entries = records.stream()
				.map(record -> new PerformanceHistoryEntry(
						record.exerciseName(),
						record.recordType().name(),
						record.recordQualifier(),
						record.achievedAt(),
						record.scheduledDate()))
				.toList();
		return CoachOverviewSection.available(new PerformanceHistoryData(entries));
	}

}
