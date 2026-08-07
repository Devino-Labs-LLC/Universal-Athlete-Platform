package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyReadinessAssessmentResult;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDataSufficiency;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.ReadinessReasonCode;

record DailyReadinessAssessmentResponse(
		UUID assessmentId,
		LocalDate stateDate,
		UUID dailyAthleteStateSnapshotId,
		int dailyAthleteStateSnapshotVersion,
		ReadinessAlgorithmVersion algorithmVersion,
		BigDecimal readinessScore,
		ReadinessBand readinessBand,
		ReadinessDataSufficiency dataSufficiency,
		ReadinessReasonCode summaryReasonCode,
		int limitingDimensionCount,
		int contributingDimensionCount,
		List<ReadinessDimensionType> limitingDimensions,
		List<ReadinessDimensionType> strongestDimensions,
		List<ReadinessDimensionContributionResponse> contributions,
		ReadinessContextResponse context,
		Instant assessedAt,
		Instant createdAt,
		boolean newlyCreated) {

	static DailyReadinessAssessmentResponse from(DailyReadinessAssessmentResult result) {
		return new DailyReadinessAssessmentResponse(
				result.assessmentId(),
				result.stateDate(),
				result.dailyAthleteStateSnapshotId(),
				result.dailyAthleteStateSnapshotVersion(),
				result.algorithmVersion(),
				result.readinessScore(),
				result.readinessBand(),
				result.dataSufficiency(),
				result.summaryReasonCode(),
				result.limitingDimensionCount(),
				result.contributingDimensionCount(),
				result.limitingDimensions(),
				result.strongestDimensions(),
				result.contributions().stream().map(ReadinessDimensionContributionResponse::from).toList(),
				ReadinessContextResponse.from(result.context()),
				result.assessedAt(),
				result.createdAt(),
				result.newlyCreated());
	}

}

record ReadinessDimensionContributionResponse(
		com.devinolabs.uap.training.domain.ReadinessDimensionType dimensionType,
		com.devinolabs.uap.training.domain.RecoveryMetricType sourceMetricType,
		boolean available,
		com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency baselineSufficiency,
		BigDecimal targetValue,
		BigDecimal baselineMean,
		BigDecimal standardizedDeviation,
		com.devinolabs.uap.training.domain.RecoveryComparisonBand comparisonBand,
		BigDecimal normalizedScore,
		BigDecimal configuredWeight,
		BigDecimal effectiveWeight,
		BigDecimal weightedContribution,
		ReadinessReasonCode reasonCode,
		Integer rankAsLimiting,
		Integer rankAsStrongest) {

	static ReadinessDimensionContributionResponse from(
			com.devinolabs.uap.training.domain.ReadinessDimensionContribution contribution) {
		return new ReadinessDimensionContributionResponse(
				contribution.dimensionType(),
				contribution.sourceMetricType(),
				contribution.available(),
				contribution.baselineSufficiency(),
				contribution.targetValue(),
				contribution.baselineMean(),
				contribution.standardizedDeviation(),
				contribution.comparisonBand(),
				contribution.normalizedScore(),
				contribution.configuredWeight(),
				contribution.effectiveWeight(),
				contribution.weightedContribution(),
				contribution.reasonCode(),
				contribution.rankAsLimiting(),
				contribution.rankAsStrongest());
	}

}

record ReadinessContextResponse(
		boolean discomfortPresent,
		List<DailyAthleteStateDiscomfortResponse> discomfortObservations,
		BigDecimal totalVolumeKilograms,
		BigDecimal totalDistanceMeters,
		long totalDurationSeconds,
		BigDecimal totalSessionRpeLoad,
		long scheduledOccurrenceCount,
		long completedScheduledCount,
		long inProgressScheduledCount,
		long skippedScheduledCount,
		long cancelledScheduledCount) {

	static ReadinessContextResponse from(DailyReadinessAssessmentResult.ReadinessContextResult context) {
		return new ReadinessContextResponse(
				context.discomfortPresent(),
				context.discomfortObservations().stream()
						.map(d -> new DailyAthleteStateDiscomfortResponse(
								d.bodyArea(), d.bodySide(), d.intensity(), d.notes(), d.orderIndex()))
						.toList(),
				context.totalVolumeKilograms(),
				context.totalDistanceMeters(),
				context.totalDurationSeconds(),
				context.totalSessionRpeLoad(),
				context.scheduledOccurrenceCount(),
				context.completedScheduledCount(),
				context.inProgressScheduledCount(),
				context.skippedScheduledCount(),
				context.cancelledScheduledCount());
	}

}
