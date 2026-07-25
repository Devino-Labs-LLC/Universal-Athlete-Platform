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
import com.devinolabs.uap.training.domain.WorkoutSessionStatus;

@Entity
@Table(name = "workout_sessions")
class WorkoutSessionJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_exercise_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutExerciseId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_day_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutDayId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private WorkoutSessionStatus status;

	@Column(name = "actual_sets")
	private Integer actualSets;

	@Column(name = "actual_reps")
	private Integer actualReps;

	@Column(name = "actual_weight", precision = 12, scale = 4)
	private BigDecimal actualWeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "weight_unit", length = 16)
	private WeightUnit weightUnit;

	@Column(name = "actual_duration_seconds")
	private Integer actualDurationSeconds;

	@Column(name = "actual_distance", precision = 12, scale = 4)
	private BigDecimal actualDistance;

	@Enumerated(EnumType.STRING)
	@Column(name = "distance_unit", length = 16)
	private DistanceUnit distanceUnit;

	@Column(name = "actual_rest_seconds")
	private Integer actualRestSeconds;

	@Column(name = "actual_rpe")
	private Integer actualRpe;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "athlete_notes", length = 4000)
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

	protected WorkoutSessionJpaEntity() {
	}

	WorkoutSessionJpaEntity(
			UUID id,
			UUID workoutExerciseId,
			UUID workoutDayId,
			UUID athleteId,
			WorkoutSessionStatus status,
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			Integer actualRpe,
			Instant completedAt,
			String athleteNotes,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.workoutExerciseId = workoutExerciseId;
		this.workoutDayId = workoutDayId;
		this.athleteId = athleteId;
		this.status = status;
		this.actualSets = actualSets;
		this.actualReps = actualReps;
		this.actualWeight = actualWeight;
		this.weightUnit = weightUnit;
		this.actualDurationSeconds = actualDurationSeconds;
		this.actualDistance = actualDistance;
		this.distanceUnit = distanceUnit;
		this.actualRestSeconds = actualRestSeconds;
		this.actualRpe = actualRpe;
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

	UUID getWorkoutExerciseId() {
		return workoutExerciseId;
	}

	UUID getWorkoutDayId() {
		return workoutDayId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	WorkoutSessionStatus getStatus() {
		return status;
	}

	Integer getActualSets() {
		return actualSets;
	}

	Integer getActualReps() {
		return actualReps;
	}

	BigDecimal getActualWeight() {
		return actualWeight;
	}

	WeightUnit getWeightUnit() {
		return weightUnit;
	}

	Integer getActualDurationSeconds() {
		return actualDurationSeconds;
	}

	BigDecimal getActualDistance() {
		return actualDistance;
	}

	DistanceUnit getDistanceUnit() {
		return distanceUnit;
	}

	Integer getActualRestSeconds() {
		return actualRestSeconds;
	}

	Integer getActualRpe() {
		return actualRpe;
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
