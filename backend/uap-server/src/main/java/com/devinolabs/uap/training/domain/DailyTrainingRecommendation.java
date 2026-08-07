package com.devinolabs.uap.training.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Immutable daily training recommendation bound to one readiness assessment and algorithm version.
 */
public final class DailyTrainingRecommendation {

	private final DailyTrainingRecommendationId id;
	private final AthleteId athleteId;
	private final LocalDate stateDate;
	private final DailyReadinessAssessmentId dailyReadinessAssessmentId;
	private final DailyAthleteStateSnapshotId dailyAthleteStateSnapshotId;
	private final int dailyAthleteStateSnapshotVersion;
	private final TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion;
	private final TrainingRecommendationAction overallAction;
	private final TrainingRecommendationStatus recommendationStatus;
	private final TrainingRecommendationReasonCode primaryReasonCode;
	private final boolean scheduledTrainingPresent;
	private final int scheduledOccurrenceCount;
	private final int modifiableScheduledOccurrenceCount;
	private final int adjustmentCount;
	private final int limitingDimensionCount;
	private final Instant generatedAt;
	private final Instant createdAt;
	private final List<TrainingRecommendationAdjustment> adjustments;
	private final List<TrainingRecommendationOccurrenceContext> occurrenceContexts;

	private DailyTrainingRecommendation(
			DailyTrainingRecommendationId id,
			AthleteId athleteId,
			LocalDate stateDate,
			DailyReadinessAssessmentId dailyReadinessAssessmentId,
			DailyAthleteStateSnapshotId dailyAthleteStateSnapshotId,
			int dailyAthleteStateSnapshotVersion,
			TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
			TrainingRecommendationAction overallAction,
			TrainingRecommendationStatus recommendationStatus,
			TrainingRecommendationReasonCode primaryReasonCode,
			boolean scheduledTrainingPresent,
			int scheduledOccurrenceCount,
			int modifiableScheduledOccurrenceCount,
			int adjustmentCount,
			int limitingDimensionCount,
			Instant generatedAt,
			Instant createdAt,
			List<TrainingRecommendationAdjustment> adjustments,
			List<TrainingRecommendationOccurrenceContext> occurrenceContexts) {
		this.id = Objects.requireNonNull(id);
		this.athleteId = Objects.requireNonNull(athleteId);
		this.stateDate = Objects.requireNonNull(stateDate);
		this.dailyReadinessAssessmentId = Objects.requireNonNull(dailyReadinessAssessmentId);
		this.dailyAthleteStateSnapshotId = Objects.requireNonNull(dailyAthleteStateSnapshotId);
		if (dailyAthleteStateSnapshotVersion < 1) {
			throw new IllegalArgumentException("dailyAthleteStateSnapshotVersion must be >= 1");
		}
		this.dailyAthleteStateSnapshotVersion = dailyAthleteStateSnapshotVersion;
		this.recommendationAlgorithmVersion = Objects.requireNonNull(recommendationAlgorithmVersion);
		this.overallAction = Objects.requireNonNull(overallAction);
		this.recommendationStatus = Objects.requireNonNull(recommendationStatus);
		this.primaryReasonCode = Objects.requireNonNull(primaryReasonCode);
		this.scheduledTrainingPresent = scheduledTrainingPresent;
		this.scheduledOccurrenceCount = scheduledOccurrenceCount;
		this.modifiableScheduledOccurrenceCount = modifiableScheduledOccurrenceCount;
		this.adjustmentCount = adjustmentCount;
		this.limitingDimensionCount = limitingDimensionCount;
		this.generatedAt = Objects.requireNonNull(generatedAt);
		this.createdAt = Objects.requireNonNull(createdAt);
		this.adjustments = List.copyOf(adjustments);
		this.occurrenceContexts = List.copyOf(occurrenceContexts);
	}

	public static DailyTrainingRecommendation create(
			DailyReadinessAssessment assessment,
			TrainingRecommendationCalculator.CalculationResult calculation) {
		Objects.requireNonNull(assessment, "assessment must not be null");
		Objects.requireNonNull(calculation, "calculation must not be null");
		return new DailyTrainingRecommendation(
				DailyTrainingRecommendationId.generate(),
				assessment.athleteId(),
				assessment.stateDate(),
				assessment.id(),
				assessment.dailyAthleteStateSnapshotId(),
				assessment.dailyAthleteStateSnapshotVersion(),
				TrainingRecommendationCalculator.ALGORITHM_VERSION,
				calculation.overallAction(),
				calculation.recommendationStatus(),
				calculation.primaryReasonCode(),
				calculation.scheduledTrainingPresent(),
				calculation.scheduledOccurrenceCount(),
				calculation.modifiableScheduledOccurrenceCount(),
				calculation.adjustments().size(),
				calculation.limitingDimensions().size(),
				calculation.generatedAt(),
				calculation.generatedAt(),
				calculation.adjustments(),
				calculation.occurrenceContexts());
	}

	public static DailyTrainingRecommendation rehydrate(
			DailyTrainingRecommendationId id,
			AthleteId athleteId,
			LocalDate stateDate,
			DailyReadinessAssessmentId dailyReadinessAssessmentId,
			DailyAthleteStateSnapshotId dailyAthleteStateSnapshotId,
			int dailyAthleteStateSnapshotVersion,
			TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
			TrainingRecommendationAction overallAction,
			TrainingRecommendationStatus recommendationStatus,
			TrainingRecommendationReasonCode primaryReasonCode,
			boolean scheduledTrainingPresent,
			int scheduledOccurrenceCount,
			int modifiableScheduledOccurrenceCount,
			int adjustmentCount,
			int limitingDimensionCount,
			Instant generatedAt,
			Instant createdAt,
			List<TrainingRecommendationAdjustment> adjustments,
			List<TrainingRecommendationOccurrenceContext> occurrenceContexts) {
		return new DailyTrainingRecommendation(
				id,
				athleteId,
				stateDate,
				dailyReadinessAssessmentId,
				dailyAthleteStateSnapshotId,
				dailyAthleteStateSnapshotVersion,
				recommendationAlgorithmVersion,
				overallAction,
				recommendationStatus,
				primaryReasonCode,
				scheduledTrainingPresent,
				scheduledOccurrenceCount,
				modifiableScheduledOccurrenceCount,
				adjustmentCount,
				limitingDimensionCount,
				generatedAt,
				createdAt,
				adjustments,
				occurrenceContexts);
	}

	public DailyTrainingRecommendationId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public LocalDate stateDate() {
		return stateDate;
	}

	public DailyReadinessAssessmentId dailyReadinessAssessmentId() {
		return dailyReadinessAssessmentId;
	}

	public DailyAthleteStateSnapshotId dailyAthleteStateSnapshotId() {
		return dailyAthleteStateSnapshotId;
	}

	public int dailyAthleteStateSnapshotVersion() {
		return dailyAthleteStateSnapshotVersion;
	}

	public TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion() {
		return recommendationAlgorithmVersion;
	}

	public TrainingRecommendationAction overallAction() {
		return overallAction;
	}

	public TrainingRecommendationStatus recommendationStatus() {
		return recommendationStatus;
	}

	public TrainingRecommendationReasonCode primaryReasonCode() {
		return primaryReasonCode;
	}

	public boolean scheduledTrainingPresent() {
		return scheduledTrainingPresent;
	}

	public int scheduledOccurrenceCount() {
		return scheduledOccurrenceCount;
	}

	public int modifiableScheduledOccurrenceCount() {
		return modifiableScheduledOccurrenceCount;
	}

	public int adjustmentCount() {
		return adjustmentCount;
	}

	public int limitingDimensionCount() {
		return limitingDimensionCount;
	}

	public Instant generatedAt() {
		return generatedAt;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public List<TrainingRecommendationAdjustment> adjustments() {
		return adjustments;
	}

	public List<TrainingRecommendationOccurrenceContext> occurrenceContexts() {
		return occurrenceContexts;
	}

}
