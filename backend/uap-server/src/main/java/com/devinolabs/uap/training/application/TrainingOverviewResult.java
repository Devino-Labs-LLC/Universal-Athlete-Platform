package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.PersonalRecordMeasure;
import com.devinolabs.uap.training.domain.PersonalRecordType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

public record TrainingOverviewResult(
		LocalDate date,
		List<ActivePlanSummary> activePlans,
		List<UpcomingOccurrenceSummary> upcomingOccurrences,
		WeeklyLoadSummary weeklyLoadSummary,
		List<CompletedSessionSummary> recentCompletedSessions,
		List<PersonalRecordBrief> recentPersonalRecords,
		List<EnvironmentSummary> activeEnvironments,
		List<OutstandingAdaptationSummary> outstandingAdaptationProposals) {

	public record ActivePlanSummary(
			UUID trainingPlanId,
			String name,
			TrainingPlanType type,
			TrainingPlanStatus status,
			LocalDate startDate,
			LocalDate endDate,
			String scheduleTimezone) {
	}

	public record UpcomingOccurrenceSummary(
			UUID occurrenceId,
			UUID trainingPlanId,
			String trainingPlanName,
			UUID workoutDayId,
			String workoutDayName,
			LocalDate scheduledDate,
			WorkoutOccurrenceStatus status,
			int exerciseCount,
			int completedExerciseCount) {
	}

	public record WeeklyLoadSummary(
			LocalDate weekStartDate,
			LocalDate weekEndDate,
			long occurrenceCount,
			long trainingDays,
			BigDecimal totalVolumeKilograms,
			long totalDurationSeconds,
			BigDecimal totalDistanceMeters,
			BigDecimal totalSessionRpeLoad,
			BigDecimal averageSessionRpe) {
	}

	public record CompletedSessionSummary(
			UUID occurrenceId,
			UUID trainingPlanId,
			String trainingPlanName,
			UUID workoutDayId,
			String workoutDayName,
			LocalDate scheduledDate,
			Instant completedAt,
			int exerciseCount,
			int completedExerciseCount) {
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

	public record EnvironmentSummary(
			UUID trainingEnvironmentId,
			String name,
			TrainingEnvironmentType type,
			boolean defaultEnvironment,
			int availableEquipmentCount) {
	}

	public record OutstandingAdaptationSummary(
			UUID adaptationProposalId,
			UUID occurrenceId,
			WorkoutAdaptationProposalStatus status,
			int unresolvedCount,
			Instant generatedAt,
			Instant expiresAt) {
	}

}
