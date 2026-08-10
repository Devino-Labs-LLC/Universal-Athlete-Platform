package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendationId;
import com.devinolabs.uap.training.domain.ReadinessCalculator;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationCalculator;

@Service
public class GetRecoveryOverviewUseCase {

	private final AthleteContextPort athleteContextPort;
	private final GetRecoveryBaselineDashboardUseCase baselineDashboardUseCase;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;
	private final DailyReadinessAssessmentRepository readinessRepository;
	private final DailyTrainingRecommendationRepository recommendationRepository;
	private final Clock clock;

	public GetRecoveryOverviewUseCase(
			AthleteContextPort athleteContextPort,
			GetRecoveryBaselineDashboardUseCase baselineDashboardUseCase,
			DailyAthleteStateSnapshotRepository snapshotRepository,
			DailyReadinessAssessmentRepository readinessRepository,
			DailyTrainingRecommendationRepository recommendationRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.baselineDashboardUseCase = Objects.requireNonNull(baselineDashboardUseCase);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
		this.readinessRepository = Objects.requireNonNull(readinessRepository);
		this.recommendationRepository = Objects.requireNonNull(recommendationRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public RecoveryOverviewResult execute(AccountId accountId, LocalDate optionalDate, Integer trendDays) {
		try {
			return load(accountId, optionalDate, trendDays);
		}
		catch (InvalidTrainingClientDateException
				| InvalidTrainingClientTrendDaysException
				| AthleteNotFoundException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new RecoveryOverviewLoadFailedException("Failed to load recovery overview", ex);
		}
	}

	private RecoveryOverviewResult load(AccountId accountId, LocalDate optionalDate, Integer trendDays) {
		LocalDate date = TrainingClientFacadeSupport.resolveDate(optionalDate, clock);
		int resolvedTrendDays = TrainingClientFacadeSupport.requireTrendDays(trendDays);
		AthleteRef athlete = TrainingClientFacadeSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());

		RecoveryBaselineDashboardResult baseline = baselineDashboardUseCase.execute(
				accountId,
				date,
				resolvedTrendDays,
				true);

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

		List<ReadinessDimensionType> limitingDimensions = readiness
				.map(r -> readinessRepository.findLimitingDimensionsByAssessmentId(
						DailyReadinessAssessmentId.of(r.assessmentId()), athleteId))
				.orElse(List.of());
		List<TrainingAdjustmentType> adjustmentTypes = recommendation
				.map(r -> recommendationRepository.findAdjustmentTypesByRecommendationId(
						DailyTrainingRecommendationId.of(r.recommendationId()), athleteId))
				.orElse(List.of());

		DailyRecoveryCheckInResult checkInResult = baseline.checkIn();
		List<RecoveryOverviewResult.DiscomfortSummary> discomfort = checkInResult == null
				? List.of()
				: checkInResult.discomfortAreas().stream()
						.map(GetRecoveryOverviewUseCase::toDiscomfort)
						.toList();

		return new RecoveryOverviewResult(
				date,
				resolvedTrendDays,
				baseline.checkInPresent(),
				toCheckInSummary(checkInResult),
				baseline.baselines(),
				baseline.metricDeviations(),
				readiness.isPresent(),
				toReadiness(readiness.orElse(null), limitingDimensions),
				recommendation.isPresent(),
				toRecommendation(recommendation.orElse(null), adjustmentTypes),
				baseline.metricTrends().stream()
						.map(trend -> new RecoveryOverviewResult.TrendSummary(
								trend.metricType(),
								trend.trendDirection(),
								trend.observationCount()))
						.toList(),
				discomfort,
				baseline.trainingLoadContext());
	}

	private static RecoveryOverviewResult.CheckInSummary toCheckInSummary(DailyRecoveryCheckInResult checkIn) {
		if (checkIn == null) {
			return null;
		}
		return new RecoveryOverviewResult.CheckInSummary(
				checkIn.id().value(),
				checkIn.completeness(),
				checkIn.fatigue() == null ? null : checkIn.fatigue().value(),
				checkIn.muscleSoreness() == null ? null : checkIn.muscleSoreness().value(),
				checkIn.stress() == null ? null : checkIn.stress().value(),
				checkIn.mood() == null ? null : checkIn.mood().value(),
				checkIn.motivation() == null ? null : checkIn.motivation().value(),
				checkIn.sleepDurationMinutes(),
				checkIn.sleepQuality() == null ? null : checkIn.sleepQuality().value(),
				!checkIn.discomfortAreas().isEmpty());
	}

	private static RecoveryOverviewResult.ReadinessSummary toReadiness(
			DailyReadinessAssessmentSummary readiness,
			List<ReadinessDimensionType> limitingDimensions) {
		if (readiness == null) {
			return null;
		}
		return new RecoveryOverviewResult.ReadinessSummary(
				readiness.assessmentId(),
				readiness.readinessScore(),
				readiness.readinessBand(),
				readiness.dataSufficiency(),
				limitingDimensions);
	}

	private static RecoveryOverviewResult.RecommendationSummary toRecommendation(
			DailyTrainingRecommendationSummary recommendation,
			List<TrainingAdjustmentType> adjustmentTypes) {
		if (recommendation == null) {
			return null;
		}
		return new RecoveryOverviewResult.RecommendationSummary(
				recommendation.recommendationId(),
				recommendation.overallAction(),
				recommendation.recommendationStatus(),
				adjustmentTypes);
	}

	private static RecoveryOverviewResult.DiscomfortSummary toDiscomfort(BodyAreaDiscomfortObservation observation) {
		return new RecoveryOverviewResult.DiscomfortSummary(
				observation.bodyArea(),
				observation.side(),
				observation.intensity().value(),
				observation.notes(),
				observation.orderIndex());
	}

}
