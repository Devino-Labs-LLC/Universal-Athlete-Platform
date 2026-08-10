package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.TrainingOverviewResult;
import com.devinolabs.uap.training.domain.PersonalRecordMeasure;
import com.devinolabs.uap.training.domain.PersonalRecordType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

record TrainingOverviewResponse(
		LocalDate date,
		List<TrainingOverviewPlanResponse> activePlans,
		List<TrainingOverviewOccurrenceResponse> upcomingOccurrences,
		TrainingOverviewWeeklyLoadResponse weeklyLoadSummary,
		List<TrainingOverviewCompletedSessionResponse> recentCompletedSessions,
		List<TrainingOverviewPersonalRecordResponse> recentPersonalRecords,
		List<TrainingOverviewEnvironmentResponse> activeEnvironments,
		List<TrainingOverviewAdaptationResponse> outstandingAdaptationProposals) {

	static TrainingOverviewResponse from(TrainingOverviewResult result) {
		return new TrainingOverviewResponse(
				result.date(),
				result.activePlans().stream().map(TrainingOverviewPlanResponse::from).toList(),
				result.upcomingOccurrences().stream().map(TrainingOverviewOccurrenceResponse::from).toList(),
				result.weeklyLoadSummary() == null
						? null
						: TrainingOverviewWeeklyLoadResponse.from(result.weeklyLoadSummary()),
				result.recentCompletedSessions().stream()
						.map(TrainingOverviewCompletedSessionResponse::from)
						.toList(),
				result.recentPersonalRecords().stream()
						.map(TrainingOverviewPersonalRecordResponse::from)
						.toList(),
				result.activeEnvironments().stream().map(TrainingOverviewEnvironmentResponse::from).toList(),
				result.outstandingAdaptationProposals().stream()
						.map(TrainingOverviewAdaptationResponse::from)
						.toList());
	}

}

record TrainingOverviewPlanResponse(
		UUID trainingPlanId,
		String name,
		TrainingPlanType type,
		TrainingPlanStatus status,
		LocalDate startDate,
		LocalDate endDate,
		String scheduleTimezone) {

	static TrainingOverviewPlanResponse from(TrainingOverviewResult.ActivePlanSummary summary) {
		return new TrainingOverviewPlanResponse(
				summary.trainingPlanId(),
				summary.name(),
				summary.type(),
				summary.status(),
				summary.startDate(),
				summary.endDate(),
				summary.scheduleTimezone());
	}
}

record TrainingOverviewOccurrenceResponse(
		UUID occurrenceId,
		UUID trainingPlanId,
		String trainingPlanName,
		UUID workoutDayId,
		String workoutDayName,
		LocalDate scheduledDate,
		WorkoutOccurrenceStatus status,
		int exerciseCount,
		int completedExerciseCount) {

	static TrainingOverviewOccurrenceResponse from(TrainingOverviewResult.UpcomingOccurrenceSummary summary) {
		return new TrainingOverviewOccurrenceResponse(
				summary.occurrenceId(),
				summary.trainingPlanId(),
				summary.trainingPlanName(),
				summary.workoutDayId(),
				summary.workoutDayName(),
				summary.scheduledDate(),
				summary.status(),
				summary.exerciseCount(),
				summary.completedExerciseCount());
	}
}

record TrainingOverviewWeeklyLoadResponse(
		LocalDate weekStartDate,
		LocalDate weekEndDate,
		long occurrenceCount,
		long trainingDays,
		BigDecimal totalVolumeKilograms,
		long totalDurationSeconds,
		BigDecimal totalDistanceMeters,
		BigDecimal totalSessionRpeLoad,
		BigDecimal averageSessionRpe) {

	static TrainingOverviewWeeklyLoadResponse from(TrainingOverviewResult.WeeklyLoadSummary summary) {
		return new TrainingOverviewWeeklyLoadResponse(
				summary.weekStartDate(),
				summary.weekEndDate(),
				summary.occurrenceCount(),
				summary.trainingDays(),
				summary.totalVolumeKilograms(),
				summary.totalDurationSeconds(),
				summary.totalDistanceMeters(),
				summary.totalSessionRpeLoad(),
				summary.averageSessionRpe());
	}
}

record TrainingOverviewCompletedSessionResponse(
		UUID occurrenceId,
		UUID trainingPlanId,
		String trainingPlanName,
		UUID workoutDayId,
		String workoutDayName,
		LocalDate scheduledDate,
		Instant completedAt,
		int exerciseCount,
		int completedExerciseCount) {

	static TrainingOverviewCompletedSessionResponse from(TrainingOverviewResult.CompletedSessionSummary summary) {
		return new TrainingOverviewCompletedSessionResponse(
				summary.occurrenceId(),
				summary.trainingPlanId(),
				summary.trainingPlanName(),
				summary.workoutDayId(),
				summary.workoutDayName(),
				summary.scheduledDate(),
				summary.completedAt(),
				summary.exerciseCount(),
				summary.completedExerciseCount());
	}
}

record TrainingOverviewPersonalRecordResponse(
		UUID personalRecordId,
		String exerciseName,
		PersonalRecordType recordType,
		String recordQualifier,
		BigDecimal normalizedValue,
		PersonalRecordMeasure normalizedUnit,
		Instant achievedAt,
		LocalDate scheduledDate,
		UUID sourceOccurrenceId) {

	static TrainingOverviewPersonalRecordResponse from(TrainingOverviewResult.PersonalRecordBrief brief) {
		return new TrainingOverviewPersonalRecordResponse(
				brief.personalRecordId(),
				brief.exerciseName(),
				brief.recordType(),
				brief.recordQualifier(),
				brief.normalizedValue(),
				brief.normalizedUnit(),
				brief.achievedAt(),
				brief.scheduledDate(),
				brief.sourceOccurrenceId());
	}
}

record TrainingOverviewEnvironmentResponse(
		UUID trainingEnvironmentId,
		String name,
		TrainingEnvironmentType type,
		boolean defaultEnvironment,
		int availableEquipmentCount) {

	static TrainingOverviewEnvironmentResponse from(TrainingOverviewResult.EnvironmentSummary summary) {
		return new TrainingOverviewEnvironmentResponse(
				summary.trainingEnvironmentId(),
				summary.name(),
				summary.type(),
				summary.defaultEnvironment(),
				summary.availableEquipmentCount());
	}
}

record TrainingOverviewAdaptationResponse(
		UUID adaptationProposalId,
		UUID occurrenceId,
		WorkoutAdaptationProposalStatus status,
		int unresolvedCount,
		Instant generatedAt,
		Instant expiresAt) {

	static TrainingOverviewAdaptationResponse from(TrainingOverviewResult.OutstandingAdaptationSummary summary) {
		return new TrainingOverviewAdaptationResponse(
				summary.adaptationProposalId(),
				summary.occurrenceId(),
				summary.status(),
				summary.unresolvedCount(),
				summary.generatedAt(),
				summary.expiresAt());
	}
}
