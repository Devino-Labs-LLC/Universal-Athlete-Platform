package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

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

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "substitution_relationship_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID substitutionRelationshipId;

	@Enumerated(EnumType.STRING)
	@Column(name = "relationship_type_snapshot", updatable = false, length = 40)
	private ExerciseSubstitutionRelationshipType relationshipTypeSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "compatibility_snapshot", updatable = false, length = 20)
	private ExerciseSubstitutionCompatibility compatibilitySnapshot;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "training_environment_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID trainingEnvironmentId;

	@Column(name = "training_environment_name_snapshot", updatable = false, length = 100)
	private String trainingEnvironmentNameSnapshot;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "workout_exercise_substitution_history_equipment_snapshot",
			joinColumns = @JoinColumn(name = "substitution_history_id", columnDefinition = "BINARY(16)"))
	@Column(name = "equipment_type", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<EquipmentType> availableEquipmentSnapshot = new LinkedHashSet<>();

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
			UUID substitutionRelationshipId,
			ExerciseSubstitutionRelationshipType relationshipTypeSnapshot,
			ExerciseSubstitutionCompatibility compatibilitySnapshot,
			UUID trainingEnvironmentId,
			String trainingEnvironmentNameSnapshot,
			Set<EquipmentType> availableEquipmentSnapshot,
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
		this.substitutionRelationshipId = substitutionRelationshipId;
		this.relationshipTypeSnapshot = relationshipTypeSnapshot;
		this.compatibilitySnapshot = compatibilitySnapshot;
		this.trainingEnvironmentId = trainingEnvironmentId;
		this.trainingEnvironmentNameSnapshot = trainingEnvironmentNameSnapshot;
		this.availableEquipmentSnapshot = availableEquipmentSnapshot == null
				? new LinkedHashSet<>()
				: new LinkedHashSet<>(availableEquipmentSnapshot);
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

	UUID getSubstitutionRelationshipId() {
		return substitutionRelationshipId;
	}

	ExerciseSubstitutionRelationshipType getRelationshipTypeSnapshot() {
		return relationshipTypeSnapshot;
	}

	ExerciseSubstitutionCompatibility getCompatibilitySnapshot() {
		return compatibilitySnapshot;
	}

	UUID getTrainingEnvironmentId() {
		return trainingEnvironmentId;
	}

	String getTrainingEnvironmentNameSnapshot() {
		return trainingEnvironmentNameSnapshot;
	}

	Set<EquipmentType> getAvailableEquipmentSnapshot() {
		return availableEquipmentSnapshot;
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
