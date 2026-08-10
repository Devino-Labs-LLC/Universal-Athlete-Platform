package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecord;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.ReadinessCalculator;
import com.devinolabs.uap.training.domain.TrainingPrimaryOccurrenceResolver;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationCalculator;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@Service
public class GetTrainingTodayDashboardUseCase {

	private static final int RECENT_PR_LIMIT = 5;
	private static final int RECENT_PR_DAYS = 7;

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;
	private final DailyReadinessAssessmentRepository readinessRepository;
	private final DailyTrainingRecommendationRepository recommendationRepository;
	private final AthleteTrainingCalendarReader calendarReader;
	private final WorkoutOccurrenceRepository occurrenceRepository;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;
	private final WorkoutAdaptationProposalRepository proposalRepository;
	private final WorkoutSessionEffortRepository sessionEffortRepository;
	private final AthleteExercisePersonalRecordRepository personalRecordRepository;
	private final WorkoutOccurrenceEquipmentFeasibilityReader equipmentFeasibilityReader;
	private final Clock clock;

	public GetTrainingTodayDashboardUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository,
			DailyAthleteStateSnapshotRepository snapshotRepository,
			DailyReadinessAssessmentRepository readinessRepository,
			DailyTrainingRecommendationRepository recommendationRepository,
			AthleteTrainingCalendarReader calendarReader,
			WorkoutOccurrenceRepository occurrenceRepository,
			TrainingLoadQueryRepository trainingLoadQueryRepository,
			WorkoutAdaptationProposalRepository proposalRepository,
			WorkoutSessionEffortRepository sessionEffortRepository,
			AthleteExercisePersonalRecordRepository personalRecordRepository,
			WorkoutOccurrenceEquipmentFeasibilityReader equipmentFeasibilityReader,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
		this.readinessRepository = Objects.requireNonNull(readinessRepository);
		this.recommendationRepository = Objects.requireNonNull(recommendationRepository);
		this.calendarReader = Objects.requireNonNull(calendarReader);
		this.occurrenceRepository = Objects.requireNonNull(occurrenceRepository);
		this.trainingLoadQueryRepository = Objects.requireNonNull(trainingLoadQueryRepository);
		this.proposalRepository = Objects.requireNonNull(proposalRepository);
		this.sessionEffortRepository = Objects.requireNonNull(sessionEffortRepository);
		this.personalRecordRepository = Objects.requireNonNull(personalRecordRepository);
		this.equipmentFeasibilityReader = Objects.requireNonNull(equipmentFeasibilityReader);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public TrainingTodayDashboardResult execute(AccountId accountId, LocalDate optionalDate) {
		try {
			return load(accountId, optionalDate);
		}
		catch (InvalidTrainingClientDateException | AthleteNotFoundException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new TrainingDashboardLoadFailedException("Failed to load training today dashboard", ex);
		}
	}

	private TrainingTodayDashboardResult load(AccountId accountId, LocalDate optionalDate) {
		LocalDate date = TrainingClientFacadeSupport.resolveDate(optionalDate, clock);
		AthleteRef athlete = TrainingClientFacadeSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());

		Optional<DailyRecoveryCheckIn> checkIn = checkInRepository.findByAthleteIdAndCheckInDate(athleteId, date);
		Optional<DailyAthleteStateSnapshotSummary> snapshot =
				snapshotRepository.findCurrentSummaryByAthleteIdAndStateDate(athleteId, date);
		Optional<DailyReadinessAssessmentSummary> readiness = snapshot.flatMap(s -> readinessRepository
				.findSummaryBySnapshotIdAndAlgorithmVersion(
						DailyAthleteStateSnapshotId.of(s.snapshotId()),
						ReadinessCalculator.ALGORITHM_VERSION,
						athleteId));
		Optional<DailyTrainingRecommendationSummary> recommendation = readiness.flatMap(r -> recommendationRepository
				.findSummaryByAssessmentIdAndAlgorithmVersion(
						DailyReadinessAssessmentId.of(r.assessmentId()),
						TrainingRecommendationCalculator.ALGORITHM_VERSION,
						athleteId));

		List<WorkoutOccurrence> occurrences = occurrenceRepository.findCalendarRange(
				athleteId, date, date, null, null);
		Map<UUID, WorkoutOccurrence> occurrenceById = new HashMap<>();
		for (WorkoutOccurrence occurrence : occurrences) {
			occurrenceById.put(occurrence.id().value(), occurrence);
		}
		List<AthleteCalendarEntryResult> calendarEntries = calendarReader.decorate(athleteId, occurrences);

		Optional<TrainingPrimaryOccurrenceResolver.OccurrenceView> primaryView =
				TrainingPrimaryOccurrenceResolver.resolve(
						occurrences.stream().map(OccurrenceAdapter::new).toList(),
						date);
		WorkoutOccurrence primaryOccurrence = primaryView
				.map(view -> occurrenceById.get(view.occurrenceId()))
				.orElse(null);

		Optional<WorkoutAdaptationProposalBrief> activeProposal = Optional.empty();
		boolean sessionEffortPresent = false;
		WorkoutFeasibilityStatus primaryFeasibility = null;
		if (primaryOccurrence != null) {
			activeProposal = proposalRepository.findActiveBriefByOccurrenceId(primaryOccurrence.id(), athleteId);
			// Effort eligibility only applies after completion — skip the exists probe otherwise.
			if (primaryOccurrence.status() == WorkoutOccurrenceStatus.COMPLETED) {
				sessionEffortPresent = sessionEffortRepository.existsByOccurrenceId(primaryOccurrence.id());
			}
			primaryFeasibility = equipmentFeasibilityReader.statusFor(athleteId, primaryOccurrence);
		}

		DailyTrainingLoadSummary dailyLoad = null;
		boolean mayHaveLoad = occurrences.stream()
				.anyMatch(occurrence -> occurrence.status() == WorkoutOccurrenceStatus.COMPLETED
						|| occurrence.status() == WorkoutOccurrenceStatus.IN_PROGRESS);
		if (mayHaveLoad) {
			List<DailyTrainingLoadSummary> dailyLoads = trainingLoadQueryRepository.aggregateDaily(
					athleteId, date, date, null, null, null, null);
			dailyLoad = dailyLoads.isEmpty() ? null : dailyLoads.getFirst();
		}

		Instant achievedFrom = Instant.now(clock).minusSeconds(RECENT_PR_DAYS * 24L * 60L * 60L);
		List<AthleteExercisePersonalRecord> recentPrs = personalRecordRepository.findRecentByAthleteId(
				athleteId, achievedFrom, RECENT_PR_LIMIT);

		TrainingRecommendationAction recommendationAction = recommendation
				.map(DailyTrainingRecommendationSummary::overallAction)
				.orElse(null);
		TrainingTodayDashboardActionsResult actions = TrainingClientActionResolver.resolveTodayActions(
				date,
				clock,
				checkIn.isPresent(),
				snapshot.isPresent(),
				readiness.isPresent(),
				recommendation.isPresent(),
				recommendationAction,
				primaryOccurrence,
				activeProposal.orElse(null),
				sessionEffortPresent);

		Map<UUID, UUID> activeProposalByOccurrence = new HashMap<>();
		activeProposal.ifPresent(proposal -> activeProposalByOccurrence.put(
				proposal.workoutOccurrenceId(),
				proposal.proposalId()));

		List<TrainingTodayDashboardResult.OccurrenceSummary> occurrenceSummaries = new ArrayList<>();
		for (AthleteCalendarEntryResult entry : calendarEntries) {
			WorkoutOccurrence entity = occurrenceById.get(entry.occurrenceId().value());
			WorkoutFeasibilityStatus feasibility = primaryOccurrence != null
					&& primaryOccurrence.id().equals(entry.occurrenceId())
							? primaryFeasibility
							: null;
			occurrenceSummaries.add(toOccurrenceSummary(
					entry,
					entity,
					feasibility,
					activeProposalByOccurrence.get(entry.occurrenceId().value())));
		}

		TrainingTodayDashboardResult.OccurrenceSummary primarySummary = null;
		if (primaryOccurrence != null) {
			primarySummary = occurrenceSummaries.stream()
					.filter(summary -> summary.occurrenceId().equals(primaryOccurrence.id().value()))
					.findFirst()
					.orElse(null);
		}

		return new TrainingTodayDashboardResult(
				date,
				new TrainingTodayDashboardResult.AthleteSection(athlete.athleteId(), "Athlete"),
				toRecovery(checkIn.orElse(null)),
				toAthleteState(snapshot.orElse(null)),
				toReadiness(readiness.orElse(null)),
				toRecommendation(recommendation.orElse(null)),
				toTraining(occurrenceSummaries, primarySummary),
				toTrainingLoad(dailyLoad),
				toAdaptation(activeProposal.orElse(null)),
				recentPrs.stream().map(GetTrainingTodayDashboardUseCase::toPrBrief).toList(),
				actions);
	}

	private static TrainingTodayDashboardResult.RecoverySection toRecovery(DailyRecoveryCheckIn checkIn) {
		if (checkIn == null) {
			return new TrainingTodayDashboardResult.RecoverySection(
					false, null, null, false, null, null, null, null, null, null, null);
		}
		return new TrainingTodayDashboardResult.RecoverySection(
				true,
				checkIn.id().value(),
				checkIn.completeness(),
				!checkIn.discomfortAreas().isEmpty(),
				checkIn.fatigue() == null ? null : checkIn.fatigue().value(),
				checkIn.muscleSoreness() == null ? null : checkIn.muscleSoreness().value(),
				checkIn.stress() == null ? null : checkIn.stress().value(),
				checkIn.mood() == null ? null : checkIn.mood().value(),
				checkIn.motivation() == null ? null : checkIn.motivation().value(),
				checkIn.sleepDurationMinutes(),
				checkIn.sleepQuality() == null ? null : checkIn.sleepQuality().value());
	}

	private static TrainingTodayDashboardResult.AthleteStateSection toAthleteState(
			DailyAthleteStateSnapshotSummary snapshot) {
		if (snapshot == null) {
			return new TrainingTodayDashboardResult.AthleteStateSection(false, null, null);
		}
		return new TrainingTodayDashboardResult.AthleteStateSection(
				true, snapshot.snapshotId(), snapshot.snapshotVersion());
	}

	private static TrainingTodayDashboardResult.ReadinessSection toReadiness(DailyReadinessAssessmentSummary readiness) {
		if (readiness == null) {
			return new TrainingTodayDashboardResult.ReadinessSection(false, null, null, null, null, List.of());
		}
		return new TrainingTodayDashboardResult.ReadinessSection(
				true,
				readiness.assessmentId(),
				readiness.readinessScore(),
				readiness.readinessBand(),
				readiness.dataSufficiency(),
				List.of());
	}

	private static TrainingTodayDashboardResult.RecommendationSection toRecommendation(
			DailyTrainingRecommendationSummary recommendation) {
		if (recommendation == null) {
			return new TrainingTodayDashboardResult.RecommendationSection(false, null, null, null, List.of());
		}
		return new TrainingTodayDashboardResult.RecommendationSection(
				true,
				recommendation.recommendationId(),
				recommendation.overallAction(),
				recommendation.recommendationStatus(),
				List.of());
	}

	private static TrainingTodayDashboardResult.TrainingSection toTraining(
			List<TrainingTodayDashboardResult.OccurrenceSummary> occurrences,
			TrainingTodayDashboardResult.OccurrenceSummary primary) {
		int scheduled = 0;
		int modifiable = 0;
		int completed = 0;
		int inProgress = 0;
		for (TrainingTodayDashboardResult.OccurrenceSummary occurrence : occurrences) {
			switch (occurrence.status()) {
				case SCHEDULED -> {
					scheduled++;
					modifiable++;
				}
				case IN_PROGRESS -> {
					inProgress++;
					modifiable++;
				}
				case COMPLETED -> completed++;
				default -> {
				}
			}
		}
		return new TrainingTodayDashboardResult.TrainingSection(
				scheduled,
				modifiable,
				completed,
				inProgress,
				occurrences,
				primary);
	}

	private static TrainingTodayDashboardResult.TrainingLoadSection toTrainingLoad(DailyTrainingLoadSummary load) {
		if (load == null) {
			return new TrainingTodayDashboardResult.TrainingLoadSection(
					false, 0, 0, 0, 0, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, null, null);
		}
		return new TrainingTodayDashboardResult.TrainingLoadSection(
				true,
				load.occurrenceCount(),
				load.ratedOccurrenceCount(),
				load.unratedOccurrenceCount(),
				load.completedExerciseCount(),
				load.completedSetCount(),
				load.totalVolumeKilograms(),
				load.totalDurationSeconds(),
				load.totalDistanceMeters(),
				load.totalSessionRpeLoad(),
				load.averageSessionRpe());
	}

	private static TrainingTodayDashboardResult.AdaptationSection toAdaptation(WorkoutAdaptationProposalBrief proposal) {
		if (proposal == null) {
			return new TrainingTodayDashboardResult.AdaptationSection(false, null, null, null, 0, null);
		}
		return new TrainingTodayDashboardResult.AdaptationSection(
				true,
				proposal.proposalId(),
				proposal.status(),
				proposal.origin(),
				proposal.unresolvedCount(),
				proposal.workoutOccurrenceId());
	}

	private static TrainingTodayDashboardResult.OccurrenceSummary toOccurrenceSummary(
			AthleteCalendarEntryResult entry,
			WorkoutOccurrence entity,
			WorkoutFeasibilityStatus feasibilityStatus,
			UUID activeProposalId) {
		UUID plannedId = null;
		String plannedName = null;
		UUID actualId = null;
		String actualName = null;
		if (entity != null) {
			if (entity.plannedEnvironment() != null) {
				plannedId = entity.plannedEnvironment().trainingEnvironmentId().value();
				plannedName = entity.plannedEnvironment().nameSnapshot();
			}
			if (entity.actualEnvironment() != null) {
				actualId = entity.actualEnvironment().trainingEnvironmentId().value();
				actualName = entity.actualEnvironment().nameSnapshot();
			}
		}
		return new TrainingTodayDashboardResult.OccurrenceSummary(
				entry.occurrenceId().value(),
				entry.trainingPlanId().value(),
				entry.workoutDayId().value(),
				entry.trainingPlanName(),
				entry.workoutDayName(),
				entry.status(),
				entry.scheduledDate(),
				entry.exerciseCount(),
				entry.completedExerciseCount(),
				entry.startedAt(),
				entry.completedAt(),
				plannedId,
				plannedName,
				actualId,
				actualName,
				feasibilityStatus,
				activeProposalId);
	}

	private static TrainingTodayDashboardResult.PersonalRecordBrief toPrBrief(AthleteExercisePersonalRecord record) {
		return new TrainingTodayDashboardResult.PersonalRecordBrief(
				record.id().value(),
				record.exerciseName(),
				record.recordType(),
				record.recordQualifier(),
				record.measurement().normalizedValue(),
				record.measurement().normalizedUnit(),
				record.achievedAt(),
				record.scheduledDate(),
				record.sourceOccurrenceId().value());
	}

	private record OccurrenceAdapter(WorkoutOccurrence occurrence)
			implements TrainingPrimaryOccurrenceResolver.OccurrenceView {

		@Override
		public UUID occurrenceId() {
			return occurrence.id().value();
		}

		@Override
		public WorkoutOccurrenceStatus status() {
			return occurrence.status();
		}

		@Override
		public LocalDate scheduledDate() {
			return occurrence.scheduledDate();
		}

		@Override
		public Instant startedAt() {
			return occurrence.startedAt();
		}

		@Override
		public Instant completedAt() {
			return occurrence.completedAt();
		}
	}

}
