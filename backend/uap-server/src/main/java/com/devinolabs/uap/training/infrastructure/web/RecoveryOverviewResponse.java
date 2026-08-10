package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.RecoveryOverviewResult;
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

record RecoveryOverviewResponse(
		LocalDate date,
		int trendDays,
		boolean checkInPresent,
		RecoveryOverviewCheckInResponse checkIn,
		List<RecoveryMetricBaselineResponse> baselines,
		List<RecoveryMetricDeviationResponse> deviations,
		boolean readinessPresent,
		RecoveryOverviewReadinessResponse readiness,
		boolean recommendationPresent,
		RecoveryOverviewRecommendationResponse recommendation,
		List<RecoveryOverviewTrendResponse> trends,
		List<RecoveryOverviewDiscomfortResponse> discomfort,
		RecoveryTrainingLoadContextResponse trainingLoadContext) {

	static RecoveryOverviewResponse from(RecoveryOverviewResult result) {
		return new RecoveryOverviewResponse(
				result.date(),
				result.trendDays(),
				result.checkInPresent(),
				result.checkIn() == null ? null : RecoveryOverviewCheckInResponse.from(result.checkIn()),
				result.baselines().stream().map(RecoveryMetricBaselineResponse::from).toList(),
				result.deviations().stream().map(RecoveryMetricDeviationResponse::from).toList(),
				result.readinessPresent(),
				result.readiness() == null ? null : RecoveryOverviewReadinessResponse.from(result.readiness()),
				result.recommendationPresent(),
				result.recommendation() == null
						? null
						: RecoveryOverviewRecommendationResponse.from(result.recommendation()),
				result.trends().stream().map(RecoveryOverviewTrendResponse::from).toList(),
				result.discomfort().stream().map(RecoveryOverviewDiscomfortResponse::from).toList(),
				RecoveryTrainingLoadContextResponse.from(result.trainingLoadContext()));
	}

}

record RecoveryOverviewCheckInResponse(
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

	static RecoveryOverviewCheckInResponse from(RecoveryOverviewResult.CheckInSummary summary) {
		return new RecoveryOverviewCheckInResponse(
				summary.recoveryCheckInId(),
				summary.completeness(),
				summary.fatigue(),
				summary.muscleSoreness(),
				summary.stress(),
				summary.mood(),
				summary.motivation(),
				summary.sleepDurationMinutes(),
				summary.sleepQuality(),
				summary.discomfortPresent());
	}
}

record RecoveryOverviewReadinessResponse(
		UUID readinessAssessmentId,
		BigDecimal readinessScore,
		ReadinessBand readinessBand,
		ReadinessDataSufficiency dataSufficiency,
		List<ReadinessDimensionType> limitingDimensions) {

	static RecoveryOverviewReadinessResponse from(RecoveryOverviewResult.ReadinessSummary summary) {
		return new RecoveryOverviewReadinessResponse(
				summary.readinessAssessmentId(),
				summary.readinessScore(),
				summary.readinessBand(),
				summary.dataSufficiency(),
				summary.limitingDimensions());
	}
}

record RecoveryOverviewRecommendationResponse(
		UUID recommendationId,
		TrainingRecommendationAction overallAction,
		TrainingRecommendationStatus recommendationStatus,
		List<TrainingAdjustmentType> adjustmentTypes) {

	static RecoveryOverviewRecommendationResponse from(RecoveryOverviewResult.RecommendationSummary summary) {
		return new RecoveryOverviewRecommendationResponse(
				summary.recommendationId(),
				summary.overallAction(),
				summary.recommendationStatus(),
				summary.adjustmentTypes());
	}
}

record RecoveryOverviewTrendResponse(
		RecoveryMetricType metricType,
		RecoveryTrendDirection trendDirection,
		int observationCount) {

	static RecoveryOverviewTrendResponse from(RecoveryOverviewResult.TrendSummary summary) {
		return new RecoveryOverviewTrendResponse(
				summary.metricType(),
				summary.trendDirection(),
				summary.observationCount());
	}
}

record RecoveryOverviewDiscomfortResponse(
		BodyArea bodyArea,
		BodySide bodySide,
		int intensity,
		String notes,
		int orderIndex) {

	static RecoveryOverviewDiscomfortResponse from(RecoveryOverviewResult.DiscomfortSummary summary) {
		return new RecoveryOverviewDiscomfortResponse(
				summary.bodyArea(),
				summary.bodySide(),
				summary.intensity(),
				summary.notes(),
				summary.orderIndex());
	}
}
