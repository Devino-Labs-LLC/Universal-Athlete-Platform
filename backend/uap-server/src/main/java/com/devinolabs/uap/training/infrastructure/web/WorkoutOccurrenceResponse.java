package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceOrigin;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

public record WorkoutOccurrenceResponse(
		UUID id,
		UUID workoutDayId,
		LocalDate scheduledDate,
		LocalTime plannedStartTime,
		Instant startedAt,
		Instant completedAt,
		WorkoutOccurrenceStatus status,
		String athleteNotes,
		WorkoutOccurrenceOrigin origin,
		LocalDate originalScheduledDate,
		boolean manuallyRescheduled,
		Instant createdAt,
		Instant updatedAt) {
}

record WorkoutOccurrenceDetailResponse(
		UUID id,
		UUID workoutDayId,
		LocalDate scheduledDate,
		LocalTime plannedStartTime,
		Instant startedAt,
		Instant completedAt,
		WorkoutOccurrenceStatus status,
		String athleteNotes,
		WorkoutOccurrenceOrigin origin,
		LocalDate originalScheduledDate,
		boolean manuallyRescheduled,
		Instant createdAt,
		Instant updatedAt,
		List<WorkoutExerciseExecutionResponse> executions) {
}

record WorkoutExerciseExecutionResponse(
		UUID id,
		UUID sourceWorkoutExerciseId,
		UUID prescribedExerciseDefinitionId,
		String prescribedExerciseName,
		UUID performedExerciseDefinitionId,
		String performedExerciseName,
		UUID exercisePerformanceKey,
		boolean substituted,
		ExerciseSubstitutionReason substitutionReason,
		String substitutionNotes,
		Instant substitutedAt,
		int displayOrder,
		String exerciseName,
		ExerciseCategory category,
		ExerciseType type,
		Integer prescribedSets,
		Integer prescribedMinimumReps,
		Integer prescribedMaximumReps,
		BigDecimal prescribedTargetWeight,
		WeightUnit prescribedWeightUnit,
		Integer prescribedTargetDurationSeconds,
		BigDecimal prescribedTargetDistance,
		DistanceUnit prescribedDistanceUnit,
		Integer prescribedTargetRestSeconds,
		Integer prescribedTargetRpe,
		String prescribedTempo,
		String prescribedCoachingNotes,
		WorkoutExerciseExecutionStatus status,
		Integer actualSets,
		Integer actualReps,
		BigDecimal actualWeight,
		WeightUnit weightUnit,
		Integer actualDurationSeconds,
		BigDecimal actualDistance,
		DistanceUnit distanceUnit,
		Integer actualRestSeconds,
		BigDecimal actualRpe,
		Instant startedAt,
		Instant completedAt,
		String athleteNotes,
		Instant createdAt,
		Instant updatedAt,
		int setCount,
		int notStartedSetCount,
		int inProgressSetCount,
		int completedSetCount,
		int skippedSetCount) {
}
