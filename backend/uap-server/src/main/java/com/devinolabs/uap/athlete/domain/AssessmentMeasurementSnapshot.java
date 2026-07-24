package com.devinolabs.uap.athlete.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable copy of an {@link AthleteMeasurement} captured when an assessment completes.
 */
public final class AssessmentMeasurementSnapshot {

	private final MeasurementType measurementType;
	private final String customMeasurementName;
	private final BigDecimal value;
	private final MeasurementUnit unit;
	private final String customUnit;
	private final MeasurementSource source;
	private final Instant measuredAt;
	private final UUID athleteSportId;
	private final UUID athleteGoalId;
	private final Instant snapshottedAt;

	private AssessmentMeasurementSnapshot(
			MeasurementType measurementType,
			String customMeasurementName,
			BigDecimal value,
			MeasurementUnit unit,
			String customUnit,
			MeasurementSource source,
			Instant measuredAt,
			UUID athleteSportId,
			UUID athleteGoalId,
			Instant snapshottedAt) {
		this.measurementType = Objects.requireNonNull(measurementType, "measurementType must not be null");
		this.customMeasurementName = customMeasurementName;
		this.value = Objects.requireNonNull(value, "value must not be null");
		this.unit = Objects.requireNonNull(unit, "unit must not be null");
		this.customUnit = customUnit;
		this.source = Objects.requireNonNull(source, "source must not be null");
		this.measuredAt = Objects.requireNonNull(measuredAt, "measuredAt must not be null");
		this.athleteSportId = athleteSportId;
		this.athleteGoalId = athleteGoalId;
		this.snapshottedAt = Objects.requireNonNull(snapshottedAt, "snapshottedAt must not be null");
	}

	public static AssessmentMeasurementSnapshot from(AthleteMeasurement source, Instant snapshottedAt) {
		Objects.requireNonNull(source, "source must not be null");
		return new AssessmentMeasurementSnapshot(
				source.measurementType(),
				source.customMeasurementName(),
				source.value(),
				source.unit(),
				source.customUnit(),
				source.source(),
				source.measuredAt(),
				source.athleteSportId() == null ? null : source.athleteSportId().value(),
				source.athleteGoalId() == null ? null : source.athleteGoalId().value(),
				snapshottedAt);
	}

	public static AssessmentMeasurementSnapshot rehydrate(
			MeasurementType measurementType,
			String customMeasurementName,
			BigDecimal value,
			MeasurementUnit unit,
			String customUnit,
			MeasurementSource source,
			Instant measuredAt,
			UUID athleteSportId,
			UUID athleteGoalId,
			Instant snapshottedAt) {
		return new AssessmentMeasurementSnapshot(
				measurementType,
				customMeasurementName,
				value,
				unit,
				customUnit,
				source,
				measuredAt,
				athleteSportId,
				athleteGoalId,
				snapshottedAt);
	}

	public MeasurementType measurementType() {
		return measurementType;
	}

	public String customMeasurementName() {
		return customMeasurementName;
	}

	public BigDecimal value() {
		return value;
	}

	public MeasurementUnit unit() {
		return unit;
	}

	public String customUnit() {
		return customUnit;
	}

	public MeasurementSource source() {
		return source;
	}

	public Instant measuredAt() {
		return measuredAt;
	}

	public UUID athleteSportId() {
		return athleteSportId;
	}

	public UUID athleteGoalId() {
		return athleteGoalId;
	}

	public Instant snapshottedAt() {
		return snapshottedAt;
	}

}
