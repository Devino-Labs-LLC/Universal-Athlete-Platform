package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Builds completed/skipped sets straight from rehydrate so metric tests do not need a database.
 */
final class PerformanceTestSets {

	static final WorkoutExerciseExecutionId EXECUTION_ID = WorkoutExerciseExecutionId.generate();

	static final WorkoutOccurrenceId OCCURRENCE_ID = WorkoutOccurrenceId.generate();

	static final AthleteId ATHLETE_ID = AthleteId.of(UUID.randomUUID());

	static final Instant CREATED_AT = Instant.parse("2026-03-01T10:00:00Z");

	private PerformanceTestSets() {
	}

	static WorkoutExerciseSet completedLift(
			WorkoutExerciseSetId id,
			int setNumber,
			Integer reps,
			String weight,
			WeightUnit unit,
			Instant completedAt) {
		return set(id, setNumber, reps, weight, unit, null, null, null, null,
				WorkoutExerciseSetStatus.COMPLETED, completedAt);
	}

	static WorkoutExerciseSet completedBodyweight(
			WorkoutExerciseSetId id,
			int setNumber,
			Integer reps,
			Instant completedAt) {
		return set(id, setNumber, reps, null, null, null, null, null, null,
				WorkoutExerciseSetStatus.COMPLETED, completedAt);
	}

	static WorkoutExerciseSet completedHold(
			WorkoutExerciseSetId id,
			int setNumber,
			Integer durationSeconds,
			Instant completedAt) {
		return set(id, setNumber, null, null, null, durationSeconds, null, null, null,
				WorkoutExerciseSetStatus.COMPLETED, completedAt);
	}

	static WorkoutExerciseSet completedDistance(
			WorkoutExerciseSetId id,
			int setNumber,
			String distance,
			DistanceUnit unit,
			Instant completedAt) {
		return set(id, setNumber, null, null, null, null, distance, unit, null,
				WorkoutExerciseSetStatus.COMPLETED, completedAt);
	}

	static WorkoutExerciseSet completedWithRpe(
			WorkoutExerciseSetId id,
			int setNumber,
			Integer reps,
			String weight,
			WeightUnit unit,
			String rpe,
			Instant completedAt) {
		return set(id, setNumber, reps, weight, unit, null, null, null, rpe,
				WorkoutExerciseSetStatus.COMPLETED, completedAt);
	}

	static WorkoutExerciseSet ineligible(
			WorkoutExerciseSetId id,
			int setNumber,
			Integer reps,
			String weight,
			WeightUnit unit,
			WorkoutExerciseSetStatus status) {
		return set(id, setNumber, reps, weight, unit, null, null, null, null, status, null);
	}

	private static WorkoutExerciseSet set(
			WorkoutExerciseSetId id,
			int setNumber,
			Integer reps,
			String weight,
			WeightUnit weightUnit,
			Integer durationSeconds,
			String distance,
			DistanceUnit distanceUnit,
			String rpe,
			WorkoutExerciseSetStatus status,
			Instant completedAt) {
		return WorkoutExerciseSet.rehydrate(
				id,
				EXECUTION_ID,
				OCCURRENCE_ID,
				ATHLETE_ID,
				setNumber,
				setNumber - 1,
				WorkoutExerciseSetType.WORKING,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				reps,
				weight == null ? null : new BigDecimal(weight),
				weightUnit,
				durationSeconds,
				distance == null ? null : new BigDecimal(distance),
				distanceUnit,
				null,
				rpe == null ? null : new BigDecimal(rpe),
				status,
				CREATED_AT,
				completedAt,
				null,
				CREATED_AT,
				CREATED_AT,
				0L);
	}

}
