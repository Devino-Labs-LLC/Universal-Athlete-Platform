package com.devinolabs.uap.athlete.infrastructure.persistence;

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

import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentType;

@Entity
@Table(name = "athlete_assessments")
class AssessmentJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_sport_id", columnDefinition = "BINARY(16)")
	private UUID athleteSportId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_goal_id", columnDefinition = "BINARY(16)")
	private UUID athleteGoalId;

	@Enumerated(EnumType.STRING)
	@Column(name = "assessment_type", nullable = false, length = 40, updatable = false)
	private AssessmentType assessmentType;

	@Column(name = "custom_type_name", length = 120, updatable = false)
	private String customTypeName;

	@Column(name = "title", nullable = false, length = 160)
	private String title;

	@Column(name = "normalized_title", nullable = false, length = 160)
	private String normalizedTitle;

	@Column(name = "description", length = 1000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AssessmentStatus status;

	@Column(name = "scheduled_at")
	private Instant scheduledAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "notes", length = 2000)
	private String notes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected AssessmentJpaEntity() {
	}

	AssessmentJpaEntity(
			UUID id,
			UUID athleteId,
			UUID athleteSportId,
			UUID athleteGoalId,
			AssessmentType assessmentType,
			String customTypeName,
			String title,
			String normalizedTitle,
			String description,
			AssessmentStatus status,
			Instant scheduledAt,
			Instant startedAt,
			Instant completedAt,
			String notes,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.athleteId = athleteId;
		this.athleteSportId = athleteSportId;
		this.athleteGoalId = athleteGoalId;
		this.assessmentType = assessmentType;
		this.customTypeName = customTypeName;
		this.title = title;
		this.normalizedTitle = normalizedTitle;
		this.description = description;
		this.status = status;
		this.scheduledAt = scheduledAt;
		this.startedAt = startedAt;
		this.completedAt = completedAt;
		this.notes = notes;
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

	UUID getAthleteSportId() {
		return athleteSportId;
	}

	UUID getAthleteGoalId() {
		return athleteGoalId;
	}

	AssessmentType getAssessmentType() {
		return assessmentType;
	}

	String getCustomTypeName() {
		return customTypeName;
	}

	String getTitle() {
		return title;
	}

	String getNormalizedTitle() {
		return normalizedTitle;
	}

	String getDescription() {
		return description;
	}

	AssessmentStatus getStatus() {
		return status;
	}

	Instant getScheduledAt() {
		return scheduledAt;
	}

	Instant getStartedAt() {
		return startedAt;
	}

	Instant getCompletedAt() {
		return completedAt;
	}

	String getNotes() {
		return notes;
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
