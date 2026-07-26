package com.devinolabs.uap.training.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
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

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetType;

@Entity
@Table(name = "workout_exercise_sets")
class WorkoutExerciseSetJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_exercise_execution_id", nullable = false, updatable = false,
			columnDefinition = "BINARY(16)")
	private UUID workoutExerciseExecutionId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_occurrence_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutOccurrenceId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "set_number", nullable = false)
	private int setNumber;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "set_type", nullable = false, length = 20)
	private WorkoutExerciseSetType setType;

	@Column(name = "prescribed_minimum_reps", updatable = false)
	private Integer prescribedMinimumReps;

	@Column(name = "prescribed_maximum_reps", updatable = false)
	private Integer prescribedMaximumReps;

	@Column(name = "prescribed_weight", precision = 12, scale = 4, updatable = false)
	private BigDecimal prescribedWeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "prescribed_weight_unit", length = 16, updatable = false)
	private WeightUnit prescribedWeightUnit;

	@Column(name = "prescribed_duration_seconds", updatable = false)
	private Integer prescribedDurationSeconds;

	@Column(name = "prescribed_distance", precision = 12, scale = 4, updatable = false)
	private BigDecimal prescribedDistance;

	@Enumerated(EnumType.STRING)
	@Column(name = "prescribed_distance_unit", length = 16, updatable = false)
	private DistanceUnit prescribedDistanceUnit;

	@Column(name = "prescribed_target_rpe", updatable = false)
	private Integer prescribedTargetRpe;

	@Column(name = "prescribed_rest_seconds", updatable = false)
	private Integer prescribedRestSeconds;

	@Column(name = "actual_reps")
	private Integer actualReps;

	@Column(name = "actual_weight", precision = 12, scale = 4)
	private BigDecimal actualWeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "actual_weight_unit", length = 16)
	private WeightUnit actualWeightUnit;

	@Column(name = "actual_duration_seconds")
	private Integer actualDurationSeconds;

	@Column(name = "actual_distance", precision = 12, scale = 4)
	private BigDecimal actualDistance;

	@Enumerated(EnumType.STRING)
	@Column(name = "actual_distance_unit", length = 16)
	private DistanceUnit actualDistanceUnit;

	@Column(name = "actual_rest_seconds")
	private Integer actualRestSeconds;

	@Column(name = "actual_rpe", precision = 12, scale = 2)
	private BigDecimal actualRpe;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private WorkoutExerciseSetStatus status;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "athlete_notes", length = 2000)
	private String athleteNotes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected WorkoutExerciseSetJpaEntity() {
	}

	WorkoutExerciseSetJpaEntity(
			UUID id,
			UUID workoutExerciseExecutionId,
			UUID workoutOccurrenceId,
			UUID athleteId,
			int setNumber,
			int displayOrder,
			WorkoutExerciseSetType setType,
			Integer prescribedMinimumReps,
			Integer prescribedMaximumReps,
			BigDecimal prescribedWeight,
			WeightUnit prescribedWeightUnit,
			Integer prescribedDurationSeconds,
			BigDecimal prescribedDistance,
			DistanceUnit prescribedDistanceUnit,
			Integer prescribedTargetRpe,
			Integer prescribedRestSeconds,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit actualWeightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit actualDistanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe,
			WorkoutExerciseSetStatus status,
			Instant startedAt,
			Instant completedAt,
			String athleteNotes,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.workoutExerciseExecutionId = workoutExerciseExecutionId;
		this.workoutOccurrenceId = workoutOccurrenceId;
		this.athleteId = athleteId;
		this.setNumber = setNumber;
		this.displayOrder = displayOrder;
		this.setType = setType;
		this.prescribedMinimumReps = prescribedMinimumReps;
		this.prescribedMaximumReps = prescribedMaximumReps;
		this.prescribedWeight = prescribedWeight;
		this.prescribedWeightUnit = prescribedWeightUnit;
		this.prescribedDurationSeconds = prescribedDurationSeconds;
		this.prescribedDistance = prescribedDistance;
		this.prescribedDistanceUnit = prescribedDistanceUnit;
		this.prescribedTargetRpe = prescribedTargetRpe;
		this.prescribedRestSeconds = prescribedRestSeconds;
		this.actualReps = actualReps;
		this.actualWeight = actualWeight;
		this.actualWeightUnit = actualWeightUnit;
		this.actualDurationSeconds = actualDurationSeconds;
		this.actualDistance = actualDistance;
		this.actualDistanceUnit = actualDistanceUnit;
		this.actualRestSeconds = actualRestSeconds;
		this.actualRpe = actualRpe;
		this.status = status;
		this.startedAt = startedAt;
		this.completedAt = completedAt;
		this.athleteNotes = athleteNotes;
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

	UUID getWorkoutExerciseExecutionId() {
		return workoutExerciseExecutionId;
	}

	UUID getWorkoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	int getSetNumber() {
		return setNumber;
	}

	int getDisplayOrder() {
		return displayOrder;
	}

	WorkoutExerciseSetType getSetType() {
		return setType;
	}

	Integer getPrescribedMinimumReps() {
		return prescribedMinimumReps;
	}

	Integer getPrescribedMaximumReps() {
		return prescribedMaximumReps;
	}

	BigDecimal getPrescribedWeight() {
		return prescribedWeight;
	}

	WeightUnit getPrescribedWeightUnit() {
		return prescribedWeightUnit;
	}

	Integer getPrescribedDurationSeconds() {
		return prescribedDurationSeconds;
	}

	BigDecimal getPrescribedDistance() {
		return prescribedDistance;
	}

	DistanceUnit getPrescribedDistanceUnit() {
		return prescribedDistanceUnit;
	}

	Integer getPrescribedTargetRpe() {
		return prescribedTargetRpe;
	}

	Integer getPrescribedRestSeconds() {
		return prescribedRestSeconds;
	}

	Integer getActualReps() {
		return actualReps;
	}

	BigDecimal getActualWeight() {
		return actualWeight;
	}

	WeightUnit getActualWeightUnit() {
		return actualWeightUnit;
	}

	Integer getActualDurationSeconds() {
		return actualDurationSeconds;
	}

	BigDecimal getActualDistance() {
		return actualDistance;
	}

	DistanceUnit getActualDistanceUnit() {
		return actualDistanceUnit;
	}

	Integer getActualRestSeconds() {
		return actualRestSeconds;
	}

	BigDecimal getActualRpe() {
		return actualRpe;
	}

	WorkoutExerciseSetStatus getStatus() {
		return status;
	}

	Instant getStartedAt() {
		return startedAt;
	}

	Instant getCompletedAt() {
		return completedAt;
	}

	String getAthleteNotes() {
		return athleteNotes;
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
