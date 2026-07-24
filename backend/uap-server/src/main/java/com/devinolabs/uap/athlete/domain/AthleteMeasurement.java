package com.devinolabs.uap.athlete.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public class AthleteMeasurement {

	private static final int SCALE = 4;
	private static final int MAX_PRECISION = 14;
	private static final int MAX_CUSTOM_NAME_LENGTH = 120;
	private static final int MAX_CUSTOM_UNIT_LENGTH = 60;
	private static final int MAX_NOTES_LENGTH = 1000;
	private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);
	private static final BigDecimal MAX_ABSOLUTE = new BigDecimal("9999999999.9999");
	private static final BigDecimal SESSION_RPE_MAX = new BigDecimal("10");

	private final AthleteMeasurementId id;
	private final AthleteId athleteId;
	private final MeasurementType measurementType;
	private final String customMeasurementName;
	private BigDecimal value;
	private MeasurementUnit unit;
	private String customUnit;
	private final MeasurementSource source;
	private String notes;
	private Instant measuredAt;
	private AthleteSportId athleteSportId;
	private AthleteGoalId athleteGoalId;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private AthleteMeasurement(
			AthleteMeasurementId id,
			AthleteId athleteId,
			MeasurementType measurementType,
			String customMeasurementName,
			BigDecimal value,
			MeasurementUnit unit,
			String customUnit,
			MeasurementSource source,
			String notes,
			Instant measuredAt,
			AthleteSportId athleteSportId,
			AthleteGoalId athleteGoalId,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.measurementType = Objects.requireNonNull(measurementType, "measurementType must not be null");
		this.customMeasurementName = normalizeCustomMeasurementName(measurementType, customMeasurementName);
		this.unit = Objects.requireNonNull(unit, "unit must not be null");
		this.customUnit = normalizeCustomUnit(unit, customUnit);
		MeasurementTypeUnitCompatibility.requireCompatible(measurementType, unit);
		this.value = normalizeAndValidateValue(measurementType, value);
		this.source = Objects.requireNonNull(source, "source must not be null");
		this.notes = normalizeNotes(notes);
		this.measuredAt = Objects.requireNonNull(measuredAt, "measuredAt must not be null");
		this.athleteSportId = athleteSportId;
		this.athleteGoalId = athleteGoalId;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static AthleteMeasurement record(
			AthleteMeasurementId id,
			AthleteId athleteId,
			MeasurementType measurementType,
			String customMeasurementName,
			BigDecimal value,
			MeasurementUnit unit,
			String customUnit,
			MeasurementSource source,
			String notes,
			Instant measuredAt,
			AthleteSportId athleteSportId,
			AthleteGoalId athleteGoalId,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		requireMeasuredAtNotTooFarInFuture(measuredAt, now);
		MeasurementSource effectiveSource = source == null ? MeasurementSource.MANUAL : source;
		return new AthleteMeasurement(
				id,
				athleteId,
				measurementType,
				customMeasurementName,
				value,
				unit,
				customUnit,
				effectiveSource,
				notes,
				measuredAt,
				athleteSportId,
				athleteGoalId,
				now,
				now,
				0L);
	}

	public static AthleteMeasurement rehydrate(
			AthleteMeasurementId id,
			AthleteId athleteId,
			MeasurementType measurementType,
			String customMeasurementName,
			BigDecimal value,
			MeasurementUnit unit,
			String customUnit,
			MeasurementSource source,
			String notes,
			Instant measuredAt,
			AthleteSportId athleteSportId,
			AthleteGoalId athleteGoalId,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new AthleteMeasurement(
				id,
				athleteId,
				measurementType,
				customMeasurementName,
				value,
				unit,
				customUnit,
				source,
				notes,
				measuredAt,
				athleteSportId,
				athleteGoalId,
				createdAt,
				updatedAt,
				version);
	}

	public void correctValue(BigDecimal value, Clock clock) {
		requireEditableClock(clock);
		this.value = normalizeAndValidateValue(measurementType, value);
		touch(clock);
	}

	public void correctUnit(MeasurementUnit unit, String customUnit, Clock clock) {
		requireEditableClock(clock);
		Objects.requireNonNull(unit, "unit must not be null");
		MeasurementTypeUnitCompatibility.requireCompatible(measurementType, unit);
		this.unit = unit;
		this.customUnit = normalizeCustomUnit(unit, customUnit);
		this.value = normalizeAndValidateValue(measurementType, this.value);
		touch(clock);
	}

	public void correctValueAndUnit(BigDecimal value, MeasurementUnit unit, String customUnit, Clock clock) {
		requireEditableClock(clock);
		Objects.requireNonNull(unit, "unit must not be null");
		MeasurementTypeUnitCompatibility.requireCompatible(measurementType, unit);
		this.unit = unit;
		this.customUnit = normalizeCustomUnit(unit, customUnit);
		this.value = normalizeAndValidateValue(measurementType, value);
		touch(clock);
	}

	public void correctMeasuredAt(Instant measuredAt, Clock clock) {
		requireEditableClock(clock);
		Objects.requireNonNull(measuredAt, "measuredAt must not be null");
		requireMeasuredAtNotTooFarInFuture(measuredAt, Instant.now(clock));
		this.measuredAt = measuredAt;
		touch(clock);
	}

	public void updateNotes(String notes, Clock clock) {
		requireEditableClock(clock);
		this.notes = normalizeNotes(notes);
		touch(clock);
	}

	public void linkSport(AthleteSportId athleteSportId, Clock clock) {
		requireEditableClock(clock);
		this.athleteSportId = Objects.requireNonNull(athleteSportId, "athleteSportId must not be null");
		touch(clock);
	}

	public void unlinkSport(Clock clock) {
		requireEditableClock(clock);
		this.athleteSportId = null;
		touch(clock);
	}

	public void linkGoal(AthleteGoalId athleteGoalId, Clock clock) {
		requireEditableClock(clock);
		this.athleteGoalId = Objects.requireNonNull(athleteGoalId, "athleteGoalId must not be null");
		touch(clock);
	}

	public void unlinkGoal(Clock clock) {
		requireEditableClock(clock);
		this.athleteGoalId = null;
		touch(clock);
	}

	private void requireEditableClock(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static void requireMeasuredAtNotTooFarInFuture(Instant measuredAt, Instant now) {
		if (measuredAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
			throw new IllegalArgumentException("measuredAt cannot be more than 5 minutes in the future");
		}
	}

	private static String normalizeCustomMeasurementName(MeasurementType type, String customMeasurementName) {
		if (type == MeasurementType.OTHER) {
			if (customMeasurementName == null || customMeasurementName.isBlank()) {
				throw new IllegalArgumentException("customMeasurementName is required when measurementType is OTHER");
			}
			String normalized = customMeasurementName.trim();
			if (normalized.length() > MAX_CUSTOM_NAME_LENGTH) {
				throw new IllegalArgumentException(
						"customMeasurementName must not exceed " + MAX_CUSTOM_NAME_LENGTH + " characters");
			}
			return normalized;
		}
		if (customMeasurementName != null && !customMeasurementName.isBlank()) {
			throw new IllegalArgumentException("customMeasurementName must be absent unless measurementType is OTHER");
		}
		return null;
	}

	private static String normalizeCustomUnit(MeasurementUnit unit, String customUnit) {
		if (unit == MeasurementUnit.OTHER) {
			if (customUnit == null || customUnit.isBlank()) {
				throw new IllegalArgumentException("customUnit is required when unit is OTHER");
			}
			String normalized = customUnit.trim();
			if (normalized.length() > MAX_CUSTOM_UNIT_LENGTH) {
				throw new IllegalArgumentException("customUnit must not exceed " + MAX_CUSTOM_UNIT_LENGTH + " characters");
			}
			return normalized;
		}
		if (customUnit != null && !customUnit.isBlank()) {
			throw new IllegalArgumentException("customUnit must be absent unless unit is OTHER");
		}
		return null;
	}

	private static String normalizeNotes(String notes) {
		if (notes == null || notes.isBlank()) {
			return null;
		}
		String normalized = notes.trim();
		if (normalized.length() > MAX_NOTES_LENGTH) {
			throw new IllegalArgumentException("notes must not exceed " + MAX_NOTES_LENGTH + " characters");
		}
		return normalized;
	}

	private static BigDecimal normalizeAndValidateValue(MeasurementType type, BigDecimal value) {
		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}
		BigDecimal normalized = value.setScale(SCALE, RoundingMode.HALF_UP);
		if (normalized.precision() > MAX_PRECISION || normalized.abs().compareTo(MAX_ABSOLUTE) > 0) {
			throw new IllegalArgumentException("value exceeds DECIMAL(14,4) capacity");
		}
		validateValueForType(type, normalized);
		return normalized;
	}

	private static void validateValueForType(MeasurementType type, BigDecimal value) {
		switch (type) {
			case BODY_WEIGHT, LEAN_BODY_MASS -> requireGreaterThanZero(value);
			case BODY_FAT_PERCENTAGE -> {
				if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
					throw new IllegalArgumentException("body fat percentage must be between 0 and 100 inclusive");
				}
			}
			case RESTING_HEART_RATE, MAX_HEART_RATE, BLOOD_PRESSURE_SYSTOLIC, BLOOD_PRESSURE_DIASTOLIC ->
					requireGreaterThanZero(value);
			case SESSION_RPE -> {
				if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(SESSION_RPE_MAX) > 0) {
					throw new IllegalArgumentException("SESSION_RPE must be between 0 and 10 inclusive");
				}
			}
			case VO2_MAX -> requireGreaterThanZero(value);
			default -> {
				if (value.compareTo(BigDecimal.ZERO) < 0) {
					throw new IllegalArgumentException("value must be greater than or equal to zero");
				}
			}
		}
	}

	private static void requireGreaterThanZero(BigDecimal value) {
		if (value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("value must be greater than zero");
		}
	}

	public AthleteMeasurementId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
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

	public String notes() {
		return notes;
	}

	public Instant measuredAt() {
		return measuredAt;
	}

	public AthleteSportId athleteSportId() {
		return athleteSportId;
	}

	public AthleteGoalId athleteGoalId() {
		return athleteGoalId;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}

}
