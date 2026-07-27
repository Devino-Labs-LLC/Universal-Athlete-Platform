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

import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;

@Entity
@Table(name = "exercise_definitions")
class ExerciseDefinitionJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "scope", nullable = false, updatable = false, length = 20)
	private ExerciseDefinitionScope scope;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "canonical_name", nullable = false, length = 150)
	private String canonicalName;

	@Column(name = "normalized_name", nullable = false, length = 150)
	private String normalizedName;

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

	protected ExerciseDefinitionJpaEntity() {
	}

	ExerciseDefinitionJpaEntity(
			UUID id,
			ExerciseDefinitionScope scope,
			UUID athleteId,
			String canonicalName,
			String normalizedName,
			boolean active,
			Instant archivedAt,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.scope = scope;
		this.athleteId = athleteId;
		this.canonicalName = canonicalName;
		this.normalizedName = normalizedName;
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

	ExerciseDefinitionScope getScope() {
		return scope;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	String getCanonicalName() {
		return canonicalName;
	}

	String getNormalizedName() {
		return normalizedName;
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
