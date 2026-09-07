package com.devinolabs.uap.training.infrastructure.persistence;

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

import com.devinolabs.uap.training.domain.TrainingAssignmentStatus;

@Entity
@Table(name = "training_assignments")
class TrainingAssignmentJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "team_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID teamId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "organization_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID organizationId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_membership_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteMembershipId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "assigned_by_account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID assignedByAccountId;

	@Column(name = "assigned_by_role", nullable = false, updatable = false, length = 20)
	private String assignedByRole;

	@Column(name = "title", nullable = false, length = 160)
	private String title;

	@Column(name = "description", length = 2000)
	private String description;

	@Column(name = "scheduled_date", nullable = false)
	private LocalDate scheduledDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private TrainingAssignmentStatus status;

	@Column(name = "athlete_response_note", length = 500)
	private String athleteResponseNote;

	@Column(name = "responded_at")
	private Instant respondedAt;

	@Column(name = "idempotency_key", nullable = false, updatable = false, length = 80)
	private String idempotencyKey;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected TrainingAssignmentJpaEntity() {
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostPersist
	@PostLoad
	void markNotNew() {
		this.isNew = false;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	UUID getTeamId() {
		return teamId;
	}

	UUID getOrganizationId() {
		return organizationId;
	}

	UUID getAthleteMembershipId() {
		return athleteMembershipId;
	}

	UUID getAssignedByAccountId() {
		return assignedByAccountId;
	}

	String getAssignedByRole() {
		return assignedByRole;
	}

	String getTitle() {
		return title;
	}

	String getDescription() {
		return description;
	}

	LocalDate getScheduledDate() {
		return scheduledDate;
	}

	TrainingAssignmentStatus getStatus() {
		return status;
	}

	String getAthleteResponseNote() {
		return athleteResponseNote;
	}

	Instant getRespondedAt() {
		return respondedAt;
	}

	String getIdempotencyKey() {
		return idempotencyKey;
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

	void apply(
			UUID id,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID athleteMembershipId,
			UUID assignedByAccountId,
			String assignedByRole,
			String title,
			String description,
			LocalDate scheduledDate,
			TrainingAssignmentStatus status,
			String athleteResponseNote,
			Instant respondedAt,
			String idempotencyKey,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.athleteId = athleteId;
		this.teamId = teamId;
		this.organizationId = organizationId;
		this.athleteMembershipId = athleteMembershipId;
		this.assignedByAccountId = assignedByAccountId;
		this.assignedByRole = assignedByRole;
		this.title = title;
		this.description = description;
		this.scheduledDate = scheduledDate;
		this.status = status;
		this.athleteResponseNote = athleteResponseNote;
		this.respondedAt = respondedAt;
		this.idempotencyKey = idempotencyKey;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
		this.isNew = isNew;
	}

}
