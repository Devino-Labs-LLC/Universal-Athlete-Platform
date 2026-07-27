package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * The athlete's current best result for one exercise performance key, record type and qualifier.
 *
 * <p>This is a projection: it is always reproducible from the completed sets it was derived from,
 * which is what makes a full rebuild safe.
 */
public class AthleteExercisePersonalRecord {

	private final AthleteExercisePersonalRecordId id;
	private final AthleteId athleteId;
	private final ExercisePerformanceKey exercisePerformanceKey;
	private final PersonalRecordType recordType;
	private final String recordQualifier;
	private String exerciseName;
	private PerformanceMeasurement measurement;
	private Integer repetitions;
	private BigDecimal weightValue;
	private WeightUnit weightUnit;
	private Instant achievedAt;
	private LocalDate scheduledDate;
	private WorkoutExerciseSetId sourceSetId;
	private WorkoutExerciseExecutionId sourceExecutionId;
	private WorkoutOccurrenceId sourceOccurrenceId;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private AthleteExercisePersonalRecord(
			AthleteExercisePersonalRecordId id,
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
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.exercisePerformanceKey = Objects.requireNonNull(
				exercisePerformanceKey, "exercisePerformanceKey must not be null");
		this.recordType = Objects.requireNonNull(recordType, "recordType must not be null");
		if (recordType.qualified() == (recordQualifier == null)) {
			throw new InvalidPerformanceMeasurementException(
					"recordQualifier is required for " + recordType + " and forbidden otherwise");
		}
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
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static AthleteExercisePersonalRecord fromCandidate(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			PersonalRecordCandidate candidate,
			PersonalRecordProvenance provenance,
			Clock clock) {
		Objects.requireNonNull(candidate, "candidate must not be null");
		Objects.requireNonNull(provenance, "provenance must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new AthleteExercisePersonalRecord(
				AthleteExercisePersonalRecordId.generate(),
				athleteId,
				exercisePerformanceKey,
				candidate.recordType(),
				candidate.recordQualifier(),
				provenance.exerciseName(),
				candidate.measurement(),
				candidate.repetitions(),
				candidate.weightValue(),
				candidate.weightUnit(),
				candidate.achievedAt(),
				provenance.scheduledDate(),
				candidate.sourceSetId(),
				provenance.executionId(),
				provenance.occurrenceId(),
				now,
				now,
				0L);
	}

	public static AthleteExercisePersonalRecord rehydrate(
			AthleteExercisePersonalRecordId id,
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
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new AthleteExercisePersonalRecord(
				id,
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
				createdAt,
				updatedAt,
				version);
	}

	/**
	 * Replaces the projected best with a candidate that has already been judged to win its slot.
	 */
	public void replaceWith(
			PersonalRecordCandidate candidate,
			PersonalRecordProvenance provenance,
			Clock clock) {
		Objects.requireNonNull(candidate, "candidate must not be null");
		Objects.requireNonNull(provenance, "provenance must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (candidate.recordType() != recordType || !Objects.equals(candidate.recordQualifier(), recordQualifier)) {
			throw new IllegalArgumentException("Candidate does not belong to this personal record slot");
		}
		this.exerciseName = provenance.exerciseName();
		this.measurement = candidate.measurement();
		this.repetitions = candidate.repetitions();
		this.weightValue = candidate.weightValue();
		this.weightUnit = candidate.weightUnit();
		this.achievedAt = candidate.achievedAt();
		this.scheduledDate = provenance.scheduledDate();
		this.sourceSetId = candidate.sourceSetId();
		this.sourceExecutionId = provenance.executionId();
		this.sourceOccurrenceId = provenance.occurrenceId();
		this.updatedAt = Instant.now(clock);
	}

	public PersonalRecordSlot slot() {
		return new PersonalRecordSlot(recordType, recordQualifier);
	}

	public BigDecimal normalizedValue() {
		return measurement.normalizedValue();
	}

	public AthleteExercisePersonalRecordId id() {
		return id;
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
