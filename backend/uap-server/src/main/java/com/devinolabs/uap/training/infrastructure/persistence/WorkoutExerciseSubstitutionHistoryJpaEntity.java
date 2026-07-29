package com.devinolabs.uap.training.infrastructure.persistence;

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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;

/**
 * Every column is {@code updatable = false} and the entity carries no version: substitution history
 * is append-only, so there is nothing to update and nothing to contend over.
 */
@Entity
@Table(name = "workout_exercise_substitution_history")
class WorkoutExerciseSubstitutionHistoryJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_occurrence_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutOccurrenceId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_exercise_execution_id", nullable = false, updatable = false,
			columnDefinition = "BINARY(16)")
	private UUID workoutExerciseExecutionId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "from_exercise_definition_id", nullable = false, updatable = false,
			columnDefinition = "BINARY(16)")
	private UUID fromExerciseDefinitionId;

	@Column(name = "from_exercise_name_snapshot", nullable = false, updatable = false, length = 160)
	private String fromExerciseNameSnapshot;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "to_exercise_definition_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID toExerciseDefinitionId;

	@Column(name = "to_exercise_name_snapshot", nullable = false, updatable = false, length = 160)
	private String toExerciseNameSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "reason", nullable = false, updatable = false, length = 32)
	private ExerciseSubstitutionReason reason;

	@Column(name = "notes", updatable = false, length = 2000)
	private String notes;

	@Column(name = "reverted", nullable = false, updatable = false)
	private boolean reverted;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private Instant changedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Transient
	private boolean isNew = true;

	protected WorkoutExerciseSubstitutionHistoryJpaEntity() {
	}

	WorkoutExerciseSubstitutionHistoryJpaEntity(
			UUID id,
			UUID athleteId,
			UUID workoutOccurrenceId,
			UUID workoutExerciseExecutionId,
			UUID fromExerciseDefinitionId,
			String fromExerciseNameSnapshot,
			UUID toExerciseDefinitionId,
			String toExerciseNameSnapshot,
			ExerciseSubstitutionReason reason,
			String notes,
			boolean reverted,
			Instant changedAt,
			Instant createdAt,
			boolean isNew) {
		this.id = id;
		this.athleteId = athleteId;
		this.workoutOccurrenceId = workoutOccurrenceId;
		this.workoutExerciseExecutionId = workoutExerciseExecutionId;
		this.fromExerciseDefinitionId = fromExerciseDefinitionId;
		this.fromExerciseNameSnapshot = fromExerciseNameSnapshot;
		this.toExerciseDefinitionId = toExerciseDefinitionId;
		this.toExerciseNameSnapshot = toExerciseNameSnapshot;
		this.reason = reason;
		this.notes = notes;
		this.reverted = reverted;
		this.changedAt = changedAt;
		this.createdAt = createdAt;
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

	UUID getWorkoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	UUID getWorkoutExerciseExecutionId() {
		return workoutExerciseExecutionId;
	}

	UUID getFromExerciseDefinitionId() {
		return fromExerciseDefinitionId;
	}

	String getFromExerciseNameSnapshot() {
		return fromExerciseNameSnapshot;
	}

	UUID getToExerciseDefinitionId() {
		return toExerciseDefinitionId;
	}

	String getToExerciseNameSnapshot() {
		return toExerciseNameSnapshot;
	}

	ExerciseSubstitutionReason getReason() {
		return reason;
	}

	String getNotes() {
		return notes;
	}

	boolean isReverted() {
		return reverted;
	}

	Instant getChangedAt() {
		return changedAt;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

}
