package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendation;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAdjustment;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationOccurrenceContext;
import com.devinolabs.uap.training.domain.TrainingRecommendationReasonCode;
import com.devinolabs.uap.training.domain.TrainingRecommendationStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

public record DailyTrainingRecommendationResult(
		UUID recommendationId,
		LocalDate stateDate,
		UUID dailyReadinessAssessmentId,
		UUID dailyAthleteStateSnapshotId,
		int dailyAthleteStateSnapshotVersion,
		TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
		TrainingRecommendationAction overallAction,
		TrainingRecommendationStatus recommendationStatus,
		TrainingRecommendationReasonCode primaryReasonCode,
		ReadinessBand readinessBand,
		BigDecimal readinessScore,
		boolean scheduledTrainingPresent,
		int scheduledOccurrenceCount,
		int modifiableScheduledOccurrenceCount,
		int adjustmentCount,
		int limitingDimensionCount,
		List<ReadinessDimensionType> limitingDimensions,
		List<TrainingRecommendationAdjustmentResult> adjustments,
		List<TrainingRecommendationOccurrenceResult> scheduledOccurrences,
		Instant generatedAt,
		Instant createdAt,
		boolean newlyCreated) {

	public static DailyTrainingRecommendationResult from(
			DailyTrainingRecommendation recommendation,
			DailyReadinessAssessment assessment,
			DailyAthleteStateSnapshot snapshot,
			boolean newlyCreated) {
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		return new DailyTrainingRecommendationResult(
				recommendation.id().value(),
				recommendation.stateDate(),
				recommendation.dailyReadinessAssessmentId().value(),
				recommendation.dailyAthleteStateSnapshotId().value(),
				recommendation.dailyAthleteStateSnapshotVersion(),
				recommendation.recommendationAlgorithmVersion(),
				recommendation.overallAction(),
				recommendation.recommendationStatus(),
				recommendation.primaryReasonCode(),
				assessment.readinessBand(),
				assessment.scoreValue(),
				recommendation.scheduledTrainingPresent(),
				recommendation.scheduledOccurrenceCount(),
				recommendation.modifiableScheduledOccurrenceCount(),
				recommendation.adjustmentCount(),
				recommendation.limitingDimensionCount(),
				assessment.limitingDimensions(),
				recommendation.adjustments().stream()
						.map(TrainingRecommendationAdjustmentResult::from)
						.toList(),
				recommendation.occurrenceContexts().stream()
						.map(TrainingRecommendationOccurrenceResult::from)
						.toList(),
				recommendation.generatedAt(),
				recommendation.createdAt(),
				newlyCreated);
	}

	public record TrainingRecommendationAdjustmentResult(
			UUID adjustmentId,
			TrainingAdjustmentType type,
			int priority,
			List<TrainingRecommendationReasonCode> reasonCodes,
			List<ReadinessDimensionType> sourceDimensions,
			String explanationKey,
			int orderIndex) {

		static TrainingRecommendationAdjustmentResult from(TrainingRecommendationAdjustment adjustment) {
			return new TrainingRecommendationAdjustmentResult(
					adjustment.id(),
					adjustment.type(),
					adjustment.priority(),
					adjustment.reasonCodes(),
					adjustment.sourceDimensions(),
					adjustment.explanationKey(),
					adjustment.orderIndex());
		}
	}

	public record TrainingRecommendationOccurrenceResult(
			UUID occurrenceId,
			UUID trainingPlanId,
			UUID workoutDayId,
			WorkoutOccurrenceStatus occurrenceStatus,
			boolean modifiable,
			String plannedEnvironmentNameSnapshot,
			String actualEnvironmentNameSnapshot,
			int orderIndex) {

		static TrainingRecommendationOccurrenceResult from(TrainingRecommendationOccurrenceContext context) {
			return new TrainingRecommendationOccurrenceResult(
					context.occurrenceId(),
					context.trainingPlanId(),
					context.workoutDayId(),
					context.occurrenceStatus(),
					context.modifiable(),
					context.plannedEnvironmentNameSnapshot(),
					context.actualEnvironmentNameSnapshot(),
					context.orderIndex());
		}
	}

}
