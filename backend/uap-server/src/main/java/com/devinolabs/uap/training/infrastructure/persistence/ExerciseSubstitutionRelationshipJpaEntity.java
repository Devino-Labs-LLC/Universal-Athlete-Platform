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
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

@Entity
@Table(name = "exercise_substitution_relationships")
class ExerciseSubstitutionRelationshipJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "owner_athlete_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID ownerAthleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "source_exercise_definition_id", nullable = false, updatable = false,
			columnDefinition = "BINARY(16)")
	private UUID sourceExerciseDefinitionId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "target_exercise_definition_id", nullable = false, updatable = false,
			columnDefinition = "BINARY(16)")
	private UUID targetExerciseDefinitionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "relationship_type", nullable = false, length = 40)
	private ExerciseSubstitutionRelationshipType relationshipType;

	@Enumerated(EnumType.STRING)
	@Column(name = "compatibility_level", nullable = false, length = 20)
	private ExerciseSubstitutionCompatibility compatibilityLevel;

	@Column(name = "rationale", length = 2000)
	private String rationale;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Column(name = "archived_at")
	private Instant archivedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected ExerciseSubstitutionRelationshipJpaEntity() {
	}

	ExerciseSubstitutionRelationshipJpaEntity(
			UUID id,
			UUID ownerAthleteId,
			UUID sourceExerciseDefinitionId,
			UUID targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipType relationshipType,
			ExerciseSubstitutionCompatibility compatibilityLevel,
			String rationale,
			boolean active,
			Instant archivedAt,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.ownerAthleteId = ownerAthleteId;
		this.sourceExerciseDefinitionId = sourceExerciseDefinitionId;
		this.targetExerciseDefinitionId = targetExerciseDefinitionId;
		this.relationshipType = relationshipType;
		this.compatibilityLevel = compatibilityLevel;
		this.rationale = rationale;
		this.active = active;
		this.archivedAt = archivedAt;
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

	UUID getOwnerAthleteId() {
		return ownerAthleteId;
	}

	UUID getSourceExerciseDefinitionId() {
		return sourceExerciseDefinitionId;
	}

	UUID getTargetExerciseDefinitionId() {
		return targetExerciseDefinitionId;
	}

	ExerciseSubstitutionRelationshipType getRelationshipType() {
		return relationshipType;
	}

	ExerciseSubstitutionCompatibility getCompatibilityLevel() {
		return compatibilityLevel;
	}

	String getRationale() {
		return rationale;
	}

	boolean isActive() {
		return active;
	}

	Instant getArchivedAt() {
		return archivedAt;
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
