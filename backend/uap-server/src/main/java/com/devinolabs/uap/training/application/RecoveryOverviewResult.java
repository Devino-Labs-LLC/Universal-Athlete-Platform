package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.BodySide;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDataSufficiency;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.RecoveryTrendDirection;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationStatus;

public record RecoveryOverviewResult(
		LocalDate date,
		int trendDays,
		boolean checkInPresent,
		CheckInSummary checkIn,
		List<RecoveryMetricBaselineResult> baselines,
		List<RecoveryMetricDeviationResult> deviations,
		boolean readinessPresent,
		ReadinessSummary readiness,
		boolean recommendationPresent,
		RecommendationSummary recommendation,
		List<TrendSummary> trends,
		List<DiscomfortSummary> discomfort,
		RecoveryTrainingLoadContextResult trainingLoadContext) {

	public record CheckInSummary(
			UUID recoveryCheckInId,
			RecoveryCheckInCompleteness completeness,
			Integer fatigue,
			Integer muscleSoreness,
			Integer stress,
			Integer mood,
			Integer motivation,
			Integer sleepDurationMinutes,
			Integer sleepQuality,
			boolean discomfortPresent) {
	}

	public record ReadinessSummary(
			UUID readinessAssessmentId,
			BigDecimal readinessScore,
			ReadinessBand readinessBand,
			ReadinessDataSufficiency dataSufficiency,
			List<ReadinessDimensionType> limitingDimensions) {
	}

	public record RecommendationSummary(
			UUID recommendationId,
			TrainingRecommendationAction overallAction,
			TrainingRecommendationStatus recommendationStatus,
			List<TrainingAdjustmentType> adjustmentTypes) {
	}

	public record TrendSummary(
			RecoveryMetricType metricType,
			RecoveryTrendDirection trendDirection,
			int observationCount) {
	}

	public record DiscomfortSummary(
			BodyArea bodyArea,
			BodySide bodySide,
			int intensity,
			String notes,
			int orderIndex) {
	}

}
