package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.training.application.PersonalRecordResult;
import com.devinolabs.uap.training.domain.PersonalRecordMeasure;
import com.devinolabs.uap.training.domain.PersonalRecordType;
import com.devinolabs.uap.training.domain.WeightUnit;

/**
 * @param normalizedValue the comparable value, always in {@code normalizedUnit}
 * @param measuredValue   the same result in the unit the athlete logged, when one applies
 * @param estimated       true only for {@code HIGHEST_ESTIMATED_ONE_REP_MAX}
 * @param exerciseDefinitionId the canonical movement; the same UUID as {@code exercisePerformanceKey}
 */
record PersonalRecordResponse(
		UUID id,
		UUID exercisePerformanceKey,
		UUID exerciseDefinitionId,
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
		UUID sourceSetId,
		UUID sourceExecutionId,
		UUID sourceOccurrenceId,
		Instant createdAt,
		Instant updatedAt) {

	static PersonalRecordResponse from(PersonalRecordResult result) {
		return new PersonalRecordResponse(
				result.id().value(),
				result.exercisePerformanceKey().value(),
				result.exercisePerformanceKey().toDefinitionId().value(),
				result.recordType(),
				result.recordQualifier(),
				result.exerciseName(),
				result.normalizedValue(),
				result.normalizedUnit(),
				result.measuredValue(),
				result.measuredUnit(),
				result.estimated(),
				result.repetitions(),
				result.weightValue(),
				result.weightUnit(),
				result.achievedAt(),
				result.scheduledDate(),
				result.sourceSetId().value(),
				result.sourceExecutionId().value(),
				result.sourceOccurrenceId().value(),
				result.createdAt(),
				result.updatedAt());
	}

}
