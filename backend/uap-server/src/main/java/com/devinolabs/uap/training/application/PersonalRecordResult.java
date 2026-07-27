package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecordId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.PersonalRecordMeasure;
import com.devinolabs.uap.training.domain.PersonalRecordType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * @param estimated true for {@code HIGHEST_ESTIMATED_ONE_REP_MAX}, whose value is calculated rather
 *                  than lifted
 */
public record PersonalRecordResult(
		AthleteExercisePersonalRecordId id,
		ExercisePerformanceKey exercisePerformanceKey,
		PersonalRecordType recordType,
		String recordQualifier,
		String exerciseName,
		BigDecimal normalizedValue,
		PersonalRecordMeasure normalizedUnit,
		BigDecimal measuredValue,
		String measuredUnit,
		boolean estimated,
		Integer repetitions,
		BigDecimal weightValue,
		WeightUnit weightUnit,
		Instant achievedAt,
		LocalDate scheduledDate,
		WorkoutExerciseSetId sourceSetId,
		WorkoutExerciseExecutionId sourceExecutionId,
		WorkoutOccurrenceId sourceOccurrenceId,
		Instant createdAt,
		Instant updatedAt) {
}
