package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.DailyAthleteStateDiscomfortSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDataSufficiency;
import com.devinolabs.uap.training.domain.ReadinessDimensionContribution;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.ReadinessReasonCode;

public record DailyReadinessAssessmentResult(
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
		List<ReadinessDimensionContribution> contributions,
		ReadinessContextResult context,
		Instant assessedAt,
		Instant createdAt,
		boolean newlyCreated) {

	public static DailyReadinessAssessmentResult from(
			DailyReadinessAssessment assessment,
			DailyAthleteStateSnapshot snapshot,
			boolean newlyCreated) {
		return new DailyReadinessAssessmentResult(
				assessment.id().value(),
				assessment.stateDate(),
				assessment.dailyAthleteStateSnapshotId().value(),
				assessment.dailyAthleteStateSnapshotVersion(),
				assessment.algorithmVersion(),
				assessment.scoreValue(),
				assessment.readinessBand(),
				assessment.dataSufficiency(),
				assessment.summaryReasonCode(),
				assessment.limitingDimensionCount(),
				assessment.contributingDimensionCount(),
				assessment.limitingDimensions(),
				assessment.strongestDimensions(),
				assessment.contributions(),
				ReadinessContextResult.from(snapshot),
				assessment.assessedAt(),
				assessment.createdAt(),
				newlyCreated);
	}

	public record ReadinessContextResult(
			boolean discomfortPresent,
			List<DailyAthleteStateDiscomfortSnapshot> discomfortObservations,
			BigDecimal totalVolumeKilograms,
			BigDecimal totalDistanceMeters,
			long totalDurationSeconds,
			BigDecimal totalSessionRpeLoad,
			long scheduledOccurrenceCount,
			long completedScheduledCount,
			long inProgressScheduledCount,
			long skippedScheduledCount,
			long cancelledScheduledCount) {

		static ReadinessContextResult from(DailyAthleteStateSnapshot snapshot) {
			return new ReadinessContextResult(
					!snapshot.discomfortObservations().isEmpty(),
					snapshot.discomfortObservations(),
					snapshot.totalVolumeKilograms(),
					snapshot.totalDistanceMeters(),
					snapshot.totalDurationSeconds(),
					snapshot.totalSessionRpeLoad(),
					snapshot.scheduledOccurrenceCount(),
					snapshot.completedScheduledCount(),
					snapshot.inProgressScheduledCount(),
					snapshot.skippedScheduledCount(),
					snapshot.cancelledScheduledCount());
		}
	}

}
