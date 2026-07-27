package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * One completed set's claim to a personal record, before it is compared with what the athlete has
 * already achieved.
 */
public record PersonalRecordCandidate(
		PersonalRecordType recordType,
		String recordQualifier,
		PerformanceMeasurement measurement,
		Integer repetitions,
		BigDecimal weightValue,
		WeightUnit weightUnit,
		WorkoutExerciseSetId sourceSetId,
		Instant achievedAt) {

	public PersonalRecordCandidate {
		Objects.requireNonNull(recordType, "recordType must not be null");
		Objects.requireNonNull(measurement, "measurement must not be null");
		Objects.requireNonNull(sourceSetId, "sourceSetId must not be null");
		Objects.requireNonNull(achievedAt, "achievedAt must not be null");
		if (recordType.qualified() == (recordQualifier == null)) {
			throw new InvalidPerformanceMeasurementException(
					"recordQualifier is required for " + recordType + " and forbidden otherwise");
		}
		if ((weightValue == null) != (weightUnit == null)) {
			throw new InvalidPerformanceMeasurementException(
					"weightValue and weightUnit must both be provided or both omitted");
		}
		if (weightValue != null) {
			weightValue = UnitNormalizationService.toMeasurementScale(weightValue);
		}
	}

	public BigDecimal normalizedValue() {
		return measurement.normalizedValue();
	}

	/**
	 * Identity of the projection slot this candidate competes for.
	 */
	public PersonalRecordSlot slot() {
		return new PersonalRecordSlot(recordType, recordQualifier);
	}

}
