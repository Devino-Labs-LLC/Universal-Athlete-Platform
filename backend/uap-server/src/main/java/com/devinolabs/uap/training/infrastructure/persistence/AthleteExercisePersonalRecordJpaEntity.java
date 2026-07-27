package com.devinolabs.uap.training.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.training.domain.PersonalRecordMeasure;
import com.devinolabs.uap.training.domain.PersonalRecordType;
import com.devinolabs.uap.training.domain.WeightUnit;

@Entity
@Table(name = "athlete_exercise_personal_records")
class AthleteExercisePersonalRecordJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "exercise_performance_key", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID exercisePerformanceKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "record_type", nullable = false, updatable = false, length = 48)
	private PersonalRecordType recordType;

	@Column(name = "record_qualifier", updatable = false, length = 64)
	private String recordQualifier;

	@Column(name = "exercise_name_snapshot", nullable = false, length = 160)
	private String exerciseName;

	@Column(name = "normalized_value", nullable = false, precision = 18, scale = 4)
	private BigDecimal normalizedValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "normalized_unit", nullable = false, length = 32)
	private PersonalRecordMeasure normalizedUnit;

	@Column(name = "measured_value", precision = 18, scale = 4)
	private BigDecimal measuredValue;

	@Column(name = "measured_unit", length = 32)
	private String measuredUnit;

	@Column(name = "estimated", nullable = false)
	private boolean estimated;

	@Column(name = "repetitions")
	private Integer repetitions;

	@Column(name = "weight_value", precision = 12, scale = 4)
	private BigDecimal weightValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "weight_unit", length = 16)
	private WeightUnit weightUnit;

	@Column(name = "achieved_at", nullable = false)
	private Instant achievedAt;

	@Column(name = "scheduled_date", nullable = false)
	private LocalDate scheduledDate;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "source_set_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID sourceSetId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "source_execution_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID sourceExecutionId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "source_occurrence_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID sourceOccurrenceId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected AthleteExercisePersonalRecordJpaEntity() {
	}

	AthleteExercisePersonalRecordJpaEntity(
			UUID id,
			UUID athleteId,
			UUID exercisePerformanceKey,
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
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.athleteId = athleteId;
		this.exercisePerformanceKey = exercisePerformanceKey;
		this.recordType = recordType;
		this.recordQualifier = recordQualifier;
		this.exerciseName = exerciseName;
		this.normalizedValue = normalizedValue;
		this.normalizedUnit = normalizedUnit;
		this.measuredValue = measuredValue;
		this.measuredUnit = measuredUnit;
		this.estimated = estimated;
		this.repetitions = repetitions;
		this.weightValue = weightValue;
		this.weightUnit = weightUnit;
		this.achievedAt = achievedAt;
		this.scheduledDate = scheduledDate;
		this.sourceSetId = sourceSetId;
		this.sourceExecutionId = sourceExecutionId;
		this.sourceOccurrenceId = sourceOccurrenceId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
		this.isNew = isNew;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	UUID getExercisePerformanceKey() {
		return exercisePerformanceKey;
	}

	PersonalRecordType getRecordType() {
		return recordType;
	}

	String getRecordQualifier() {
		return recordQualifier;
	}

	String getExerciseName() {
		return exerciseName;
	}

	BigDecimal getNormalizedValue() {
		return normalizedValue;
	}

	PersonalRecordMeasure getNormalizedUnit() {
		return normalizedUnit;
	}

	BigDecimal getMeasuredValue() {
		return measuredValue;
	}

	String getMeasuredUnit() {
		return measuredUnit;
	}

	boolean isEstimated() {
		return estimated;
	}

	Integer getRepetitions() {
		return repetitions;
	}

	BigDecimal getWeightValue() {
		return weightValue;
	}

	WeightUnit getWeightUnit() {
		return weightUnit;
	}

	Instant getAchievedAt() {
		return achievedAt;
	}

	LocalDate getScheduledDate() {
		return scheduledDate;
	}

	UUID getSourceSetId() {
		return sourceSetId;
	}

	UUID getSourceExecutionId() {
		return sourceExecutionId;
	}

	UUID getSourceOccurrenceId() {
		return sourceOccurrenceId;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	long getVersion() {
		return version;
	}

}
