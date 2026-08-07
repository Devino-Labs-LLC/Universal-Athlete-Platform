package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyTrainingRecommendationResult;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationReasonCode;
import com.devinolabs.uap.training.domain.TrainingRecommendationStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

record DailyTrainingRecommendationResponse(
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
		List<TrainingRecommendationAdjustmentResponse> adjustments,
		List<TrainingRecommendationOccurrenceResponse> scheduledOccurrences,
		Instant generatedAt,
		Instant createdAt,
		boolean newlyCreated) {

	static DailyTrainingRecommendationResponse from(DailyTrainingRecommendationResult result) {
		return new DailyTrainingRecommendationResponse(
				result.recommendationId(),
				result.stateDate(),
				result.dailyReadinessAssessmentId(),
				result.dailyAthleteStateSnapshotId(),
				result.dailyAthleteStateSnapshotVersion(),
				result.recommendationAlgorithmVersion(),
				result.overallAction(),
				result.recommendationStatus(),
				result.primaryReasonCode(),
				result.readinessBand(),
				result.readinessScore(),
				result.scheduledTrainingPresent(),
				result.scheduledOccurrenceCount(),
				result.modifiableScheduledOccurrenceCount(),
				result.adjustmentCount(),
				result.limitingDimensionCount(),
				result.limitingDimensions(),
				result.adjustments().stream().map(TrainingRecommendationAdjustmentResponse::from).toList(),
				result.scheduledOccurrences().stream().map(TrainingRecommendationOccurrenceResponse::from).toList(),
				result.generatedAt(),
				result.createdAt(),
				result.newlyCreated());
	}

}

record TrainingRecommendationAdjustmentResponse(
		UUID adjustmentId,
		TrainingAdjustmentType type,
		int priority,
		List<TrainingRecommendationReasonCode> reasonCodes,
		List<ReadinessDimensionType> sourceDimensions,
		String explanationKey,
		int orderIndex) {

	static TrainingRecommendationAdjustmentResponse from(
			DailyTrainingRecommendationResult.TrainingRecommendationAdjustmentResult adjustment) {
		return new TrainingRecommendationAdjustmentResponse(
				adjustment.adjustmentId(),
				adjustment.type(),
				adjustment.priority(),
				adjustment.reasonCodes(),
				adjustment.sourceDimensions(),
				adjustment.explanationKey(),
				adjustment.orderIndex());
	}

}

record TrainingRecommendationOccurrenceResponse(
		UUID occurrenceId,
		UUID trainingPlanId,
		UUID workoutDayId,
		WorkoutOccurrenceStatus occurrenceStatus,
		boolean modifiable,
		String plannedEnvironmentNameSnapshot,
		String actualEnvironmentNameSnapshot,
		int orderIndex) {

	static TrainingRecommendationOccurrenceResponse from(
			DailyTrainingRecommendationResult.TrainingRecommendationOccurrenceResult occurrence) {
		return new TrainingRecommendationOccurrenceResponse(
				occurrence.occurrenceId(),
				occurrence.trainingPlanId(),
				occurrence.workoutDayId(),
				occurrence.occurrenceStatus(),
				occurrence.modifiable(),
				occurrence.plannedEnvironmentNameSnapshot(),
				occurrence.actualEnvironmentNameSnapshot(),
				occurrence.orderIndex());
	}

}
