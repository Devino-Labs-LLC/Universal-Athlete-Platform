package com.devinolabs.uap.training.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "workout_session_effort_revisions")
class WorkoutSessionEffortRevisionJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_session_effort_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutSessionEffortId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "revision_number", nullable = false, updatable = false)
	private int revisionNumber;

	@Column(name = "prior_session_rpe", nullable = false, updatable = false, precision = 3, scale = 1)
	private BigDecimal priorSessionRpe;

	@Column(name = "new_session_rpe", nullable = false, updatable = false, precision = 3, scale = 1)
	private BigDecimal newSessionRpe;

	@Column(name = "prior_duration_minutes", updatable = false)
	private Integer priorDurationMinutes;

	@Column(name = "new_duration_minutes", updatable = false)
	private Integer newDurationMinutes;

	@Column(name = "prior_notes", length = 1000, updatable = false)
	private String priorNotes;

	@Column(name = "new_notes", length = 1000, updatable = false)
	private String newNotes;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private Instant changedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Transient
	private boolean isNew = true;

	protected WorkoutSessionEffortRevisionJpaEntity() {
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

	UUID getWorkoutSessionEffortId() {
		return workoutSessionEffortId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	int getRevisionNumber() {
		return revisionNumber;
	}

	BigDecimal getPriorSessionRpe() {
		return priorSessionRpe;
	}

	BigDecimal getNewSessionRpe() {
		return newSessionRpe;
	}

	Integer getPriorDurationMinutes() {
		return priorDurationMinutes;
	}

	Integer getNewDurationMinutes() {
		return newDurationMinutes;
	}

	String getPriorNotes() {
		return priorNotes;
	}

	String getNewNotes() {
		return newNotes;
	}

	Instant getChangedAt() {
		return changedAt;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	void setId(UUID id) {
		this.id = id;
	}

	void setWorkoutSessionEffortId(UUID workoutSessionEffortId) {
		this.workoutSessionEffortId = workoutSessionEffortId;
	}

	void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	void setRevisionNumber(int revisionNumber) {
		this.revisionNumber = revisionNumber;
	}

	void setPriorSessionRpe(BigDecimal priorSessionRpe) {
		this.priorSessionRpe = priorSessionRpe;
	}

	void setNewSessionRpe(BigDecimal newSessionRpe) {
		this.newSessionRpe = newSessionRpe;
	}

	void setPriorDurationMinutes(Integer priorDurationMinutes) {
		this.priorDurationMinutes = priorDurationMinutes;
	}

	void setNewDurationMinutes(Integer newDurationMinutes) {
		this.newDurationMinutes = newDurationMinutes;
	}

	void setPriorNotes(String priorNotes) {
		this.priorNotes = priorNotes;
	}

	void setNewNotes(String newNotes) {
		this.newNotes = newNotes;
	}

	void setChangedAt(Instant changedAt) {
		this.changedAt = changedAt;
	}

	void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	void setNew(boolean isNew) {
		this.isNew = isNew;
	}

}
