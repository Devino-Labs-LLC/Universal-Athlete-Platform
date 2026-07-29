package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

/**
 * {@code exerciseName} is kept as an alias for {@code performedExerciseName} so existing clients
 * keep reading the movement that was actually trained.
 */
public record WorkoutExerciseExecutionResult(
		WorkoutExerciseExecutionId id,
		WorkoutExerciseId sourceWorkoutExerciseId,
		ExerciseDefinitionId prescribedExerciseDefinitionId,
		String prescribedExerciseName,
		ExerciseDefinitionId performedExerciseDefinitionId,
		String performedExerciseName,
		ExercisePerformanceKey exercisePerformanceKey,
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
		WorkoutExerciseSetCounts setCounts) {
}
