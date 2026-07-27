package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Append-only log of every time a personal record slot was actually beaten.
 *
 * <p>Entries are never rewritten: when a later result takes the slot, the standing entry is stamped
 * with {@code supersededAt} and the id of the entry that replaced it. Exact ties add no entry.
 */
public class AthleteExercisePersonalRecordHistory {

	private final AthleteExercisePersonalRecordHistoryId id;
	private final AthleteExercisePersonalRecordId personalRecordId;
	private final AthleteId athleteId;
	private final ExercisePerformanceKey exercisePerformanceKey;
	private final PersonalRecordType recordType;
	private final String recordQualifier;
	private final String exerciseName;
	private final PerformanceMeasurement measurement;
	private final Integer repetitions;
	private final BigDecimal weightValue;
	private final WeightUnit weightUnit;
	private final Instant achievedAt;
	private final LocalDate scheduledDate;
	private final WorkoutExerciseSetId sourceSetId;
	private final WorkoutExerciseExecutionId sourceExecutionId;
	private final WorkoutOccurrenceId sourceOccurrenceId;
	private Instant supersededAt;
	private AthleteExercisePersonalRecordHistoryId supersededByHistoryId;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private AthleteExercisePersonalRecordHistory(
			AthleteExercisePersonalRecordHistoryId id,
			AthleteExercisePersonalRecordId personalRecordId,
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			PersonalRecordType recordType,
			String recordQualifier,
			String exerciseName,
			PerformanceMeasurement measurement,
			Integer repetitions,
			BigDecimal weightValue,
			WeightUnit weightUnit,
			Instant achievedAt,
			LocalDate scheduledDate,
			WorkoutExerciseSetId sourceSetId,
			WorkoutExerciseExecutionId sourceExecutionId,
			WorkoutOccurrenceId sourceOccurrenceId,
			Instant supersededAt,
			AthleteExercisePersonalRecordHistoryId supersededByHistoryId,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.personalRecordId = Objects.requireNonNull(personalRecordId, "personalRecordId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.exercisePerformanceKey = Objects.requireNonNull(
				exercisePerformanceKey, "exercisePerformanceKey must not be null");
		this.recordType = Objects.requireNonNull(recordType, "recordType must not be null");
		this.recordQualifier = recordQualifier;
		this.exerciseName = Objects.requireNonNull(exerciseName, "exerciseName must not be null");
		this.measurement = Objects.requireNonNull(measurement, "measurement must not be null");
		this.repetitions = repetitions;
		this.weightValue = weightValue;
		this.weightUnit = weightUnit;
		this.achievedAt = Objects.requireNonNull(achievedAt, "achievedAt must not be null");
		this.scheduledDate = Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
		this.sourceSetId = Objects.requireNonNull(sourceSetId, "sourceSetId must not be null");
		this.sourceExecutionId = Objects.requireNonNull(sourceExecutionId, "sourceExecutionId must not be null");
		this.sourceOccurrenceId = Objects.requireNonNull(sourceOccurrenceId, "sourceOccurrenceId must not be null");
		if ((supersededAt == null) != (supersededByHistoryId == null)) {
			throw new IllegalArgumentException(
					"supersededAt and supersededByHistoryId must both be provided or both omitted");
		}
		this.supersededAt = supersededAt;
		this.supersededByHistoryId = supersededByHistoryId;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static AthleteExercisePersonalRecordHistory append(
			AthleteExercisePersonalRecord record,
			Clock clock) {
		Objects.requireNonNull(record, "record must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new AthleteExercisePersonalRecordHistory(
				AthleteExercisePersonalRecordHistoryId.generate(),
				record.id(),
				record.athleteId(),
				record.exercisePerformanceKey(),
				record.recordType(),
				record.recordQualifier(),
				record.exerciseName(),
				record.measurement(),
				record.repetitions(),
				record.weightValue(),
				record.weightUnit(),
				record.achievedAt(),
				record.scheduledDate(),
				record.sourceSetId(),
				record.sourceExecutionId(),
				record.sourceOccurrenceId(),
				null,
				null,
				now,
				now,
				0L);
	}

	public static AthleteExercisePersonalRecordHistory rehydrate(
			AthleteExercisePersonalRecordHistoryId id,
			AthleteExercisePersonalRecordId personalRecordId,
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			PersonalRecordType recordType,
			String recordQualifier,
			String exerciseName,
			PerformanceMeasurement measurement,
			Integer repetitions,
			BigDecimal weightValue,
			WeightUnit weightUnit,
			Instant achievedAt,
			LocalDate scheduledDate,
			WorkoutExerciseSetId sourceSetId,
			WorkoutExerciseExecutionId sourceExecutionId,
			WorkoutOccurrenceId sourceOccurrenceId,
			Instant supersededAt,
			AthleteExercisePersonalRecordHistoryId supersededByHistoryId,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new AthleteExercisePersonalRecordHistory(
				id,
				personalRecordId,
				athleteId,
				exercisePerformanceKey,
				recordType,
				recordQualifier,
				exerciseName,
				measurement,
				repetitions,
				weightValue,
				weightUnit,
				achievedAt,
				scheduledDate,
				sourceSetId,
				sourceExecutionId,
				sourceOccurrenceId,
				supersededAt,
				supersededByHistoryId,
				createdAt,
				updatedAt,
				version);
	}

	public void supersede(AthleteExercisePersonalRecordHistory replacement, Clock clock) {
		Objects.requireNonNull(replacement, "replacement must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (supersededAt != null) {
			throw new IllegalStateException("Personal record history entry is already superseded");
		}
		Instant now = Instant.now(clock);
		this.supersededAt = now;
		this.supersededByHistoryId = replacement.id();
		this.updatedAt = now;
	}

	public boolean isCurrent() {
		return supersededAt == null;
	}

	public AthleteExercisePersonalRecordHistoryId id() {
		return id;
	}

	public AthleteExercisePersonalRecordId personalRecordId() {
		return personalRecordId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public ExercisePerformanceKey exercisePerformanceKey() {
		return exercisePerformanceKey;
	}

	public PersonalRecordType recordType() {
		return recordType;
	}

	public String recordQualifier() {
		return recordQualifier;
	}

	public String exerciseName() {
		return exerciseName;
	}

	public PerformanceMeasurement measurement() {
		return measurement;
	}

	public Integer repetitions() {
		return repetitions;
	}

	public BigDecimal weightValue() {
		return weightValue;
	}

	public WeightUnit weightUnit() {
		return weightUnit;
	}

	public Instant achievedAt() {
		return achievedAt;
	}

	public LocalDate scheduledDate() {
		return scheduledDate;
	}

	public WorkoutExerciseSetId sourceSetId() {
		return sourceSetId;
	}

	public WorkoutExerciseExecutionId sourceExecutionId() {
		return sourceExecutionId;
	}

	public WorkoutOccurrenceId sourceOccurrenceId() {
		return sourceOccurrenceId;
	}

	public Instant supersededAt() {
		return supersededAt;
	}

	public AthleteExercisePersonalRecordHistoryId supersededByHistoryId() {
		return supersededByHistoryId;
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
