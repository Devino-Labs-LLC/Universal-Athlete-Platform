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
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseStatus;

@Entity
@Table(name = "workout_exercises")
class WorkoutExerciseJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_day_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutDayId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "exercise_definition_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID exerciseDefinitionId;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "exercise_name", nullable = false, length = 160)
	private String exerciseName;

	@Column(name = "normalized_exercise_name", nullable = false, length = 160)
	private String normalizedExerciseName;

	@Enumerated(EnumType.STRING)
	@Column(name = "exercise_category", nullable = false, length = 32)
	private ExerciseCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "exercise_type", nullable = false, length = 32)
	private ExerciseType type;

	@Column(name = "sets", nullable = false)
	private Integer sets;

	@Column(name = "minimum_reps")
	private Integer minimumReps;

	@Column(name = "maximum_reps")
	private Integer maximumReps;

	@Column(name = "target_weight", precision = 12, scale = 4)
	private BigDecimal targetWeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "weight_unit", length = 16)
	private WeightUnit weightUnit;

	@Column(name = "target_duration_seconds")
	private Integer targetDurationSeconds;

	@Column(name = "target_distance", precision = 12, scale = 4)
	private BigDecimal targetDistance;

	@Enumerated(EnumType.STRING)
	@Column(name = "distance_unit", length = 16)
	private DistanceUnit distanceUnit;

	@Column(name = "target_rest_seconds")
	private Integer targetRestSeconds;

	@Column(name = "target_rpe")
	private Integer targetRpe;

	@Column(name = "tempo", length = 40)
	private String tempo;

	@Column(name = "coaching_notes", length = 2000)
	private String coachingNotes;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private WorkoutExerciseStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected WorkoutExerciseJpaEntity() {
	}

	WorkoutExerciseJpaEntity(
			UUID id,
			UUID workoutDayId,
			UUID athleteId,
			UUID exerciseDefinitionId,
			int displayOrder,
			String exerciseName,
			String normalizedExerciseName,
			ExerciseCategory category,
			ExerciseType type,
			Integer sets,
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal targetWeight,
			WeightUnit weightUnit,
			Integer targetDurationSeconds,
			BigDecimal targetDistance,
			DistanceUnit distanceUnit,
			Integer targetRestSeconds,
			Integer targetRpe,
			String tempo,
			String coachingNotes,
			WorkoutExerciseStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.workoutDayId = workoutDayId;
		this.athleteId = athleteId;
		this.exerciseDefinitionId = exerciseDefinitionId;
		this.displayOrder = displayOrder;
		this.exerciseName = exerciseName;
		this.normalizedExerciseName = normalizedExerciseName;
		this.category = category;
		this.type = type;
		this.sets = sets;
		this.minimumReps = minimumReps;
		this.maximumReps = maximumReps;
		this.targetWeight = targetWeight;
		this.weightUnit = weightUnit;
		this.targetDurationSeconds = targetDurationSeconds;
		this.targetDistance = targetDistance;
		this.distanceUnit = distanceUnit;
		this.targetRestSeconds = targetRestSeconds;
		this.targetRpe = targetRpe;
		this.tempo = tempo;
		this.coachingNotes = coachingNotes;
		this.status = status;
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

	UUID getWorkoutDayId() {
		return workoutDayId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	UUID getExerciseDefinitionId() {
		return exerciseDefinitionId;
	}

	int getDisplayOrder() {
		return displayOrder;
	}

	String getExerciseName() {
		return exerciseName;
	}

	String getNormalizedExerciseName() {
		return normalizedExerciseName;
	}

	ExerciseCategory getCategory() {
		return category;
	}

	ExerciseType getType() {
		return type;
	}

	Integer getSets() {
		return sets;
	}

	Integer getMinimumReps() {
		return minimumReps;
	}

	Integer getMaximumReps() {
		return maximumReps;
	}

	BigDecimal getTargetWeight() {
		return targetWeight;
	}

	WeightUnit getWeightUnit() {
		return weightUnit;
	}

	Integer getTargetDurationSeconds() {
		return targetDurationSeconds;
	}

	BigDecimal getTargetDistance() {
		return targetDistance;
	}

	DistanceUnit getDistanceUnit() {
		return distanceUnit;
	}

	Integer getTargetRestSeconds() {
		return targetRestSeconds;
	}

	Integer getTargetRpe() {
		return targetRpe;
	}

	String getTempo() {
		return tempo;
	}

	String getCoachingNotes() {
		return coachingNotes;
	}

	WorkoutExerciseStatus getStatus() {
		return status;
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
