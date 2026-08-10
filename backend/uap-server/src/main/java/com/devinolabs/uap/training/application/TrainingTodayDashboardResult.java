package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.PersonalRecordMeasure;
import com.devinolabs.uap.training.domain.PersonalRecordType;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDataSufficiency;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationStatus;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalOrigin;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

public record TrainingTodayDashboardResult(
		LocalDate date,
		AthleteSection athlete,
		RecoverySection recovery,
		AthleteStateSection athleteState,
		ReadinessSection readiness,
		RecommendationSection recommendation,
		TrainingSection training,
		TrainingLoadSection trainingLoad,
		AdaptationSection adaptation,
		List<PersonalRecordBrief> recentPerformance,
		TrainingTodayDashboardActionsResult actions) {

	public record AthleteSection(
			UUID athleteId,
			String displayName) {
	}

	public record RecoverySection(
			boolean checkInPresent,
			UUID recoveryCheckInId,
			RecoveryCheckInCompleteness completeness,
			boolean discomfortPresent,
			Integer fatigue,
			Integer muscleSoreness,
			Integer stress,
			Integer mood,
			Integer motivation,
			Integer sleepDurationMinutes,
			Integer sleepQuality) {
	}

	public record AthleteStateSection(
			boolean snapshotPresent,
			UUID dailyAthleteStateSnapshotId,
			Integer snapshotVersion) {
	}

	public record ReadinessSection(
			boolean readinessPresent,
			UUID readinessAssessmentId,
			BigDecimal readinessScore,
			ReadinessBand readinessBand,
			ReadinessDataSufficiency dataSufficiency,
			List<ReadinessDimensionType> limitingDimensions) {
	}

	public record RecommendationSection(
			boolean recommendationPresent,
			UUID recommendationId,
			TrainingRecommendationAction overallAction,
			TrainingRecommendationStatus recommendationStatus,
			List<TrainingAdjustmentType> adjustmentTypes) {
	}

	public record TrainingSection(
			int scheduledOccurrenceCount,
			int modifiableOccurrenceCount,
			int completedOccurrenceCount,
			int inProgressOccurrenceCount,
			List<OccurrenceSummary> occurrences,
			OccurrenceSummary primaryOccurrence) {
	}

	public record OccurrenceSummary(
			UUID occurrenceId,
			UUID trainingPlanId,
			UUID workoutDayId,
			String trainingPlanName,
			String workoutDayName,
			WorkoutOccurrenceStatus status,
			LocalDate scheduledDate,
			int exerciseCount,
			int completedExerciseCount,
			Instant startedAt,
			Instant completedAt,
			UUID plannedEnvironmentId,
			String plannedEnvironmentName,
			UUID actualEnvironmentId,
			String actualEnvironmentName,
			WorkoutFeasibilityStatus feasibilityStatus,
			UUID activeAdaptationProposalId) {
	}

	public record TrainingLoadSection(
			boolean loadPresent,
			long occurrenceCount,
			long ratedOccurrenceCount,
			long unratedOccurrenceCount,
			long completedExerciseCount,
			long completedSetCount,
			BigDecimal totalVolumeKilograms,
			long totalDurationSeconds,
			BigDecimal totalDistanceMeters,
			BigDecimal totalSessionRpeLoad,
			BigDecimal averageSessionRpe) {
	}

	public record AdaptationSection(
			boolean activeProposalPresent,
			UUID adaptationProposalId,
			WorkoutAdaptationProposalStatus status,
			WorkoutAdaptationProposalOrigin origin,
			int unresolvedCount,
			UUID occurrenceId) {
	}

	public record PersonalRecordBrief(
			UUID personalRecordId,
			String exerciseName,
			PersonalRecordType recordType,
			String recordQualifier,
			BigDecimal normalizedValue,
			PersonalRecordMeasure normalizedUnit,
			Instant achievedAt,
			LocalDate scheduledDate,
			UUID sourceOccurrenceId) {
	}

}
