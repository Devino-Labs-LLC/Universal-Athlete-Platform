package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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

import com.devinolabs.uap.training.domain.WorkoutOccurrenceOrigin;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@Entity
@Table(name = "workout_occurrences")
class WorkoutOccurrenceJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "training_plan_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID trainingPlanId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_day_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutDayId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "scheduled_date", nullable = false)
	private LocalDate scheduledDate;

	@Column(name = "planned_start_time")
	private LocalTime plannedStartTime;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private WorkoutOccurrenceStatus status;

	@Column(name = "athlete_notes", length = 4000)
	private String athleteNotes;

	@Enumerated(EnumType.STRING)
	@Column(name = "origin", nullable = false, length = 20, updatable = false)
	private WorkoutOccurrenceOrigin origin;

	@Column(name = "generation_key", length = 200, updatable = false)
	private String generationKey;

	@Column(name = "original_scheduled_date")
	private LocalDate originalScheduledDate;

	@Column(name = "manually_rescheduled", nullable = false)
	private boolean manuallyRescheduled;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected WorkoutOccurrenceJpaEntity() {
	}

	WorkoutOccurrenceJpaEntity(
			UUID id,
			UUID trainingPlanId,
			UUID workoutDayId,
			UUID athleteId,
			LocalDate scheduledDate,
			LocalTime plannedStartTime,
			Instant startedAt,
			Instant completedAt,
			WorkoutOccurrenceStatus status,
			String athleteNotes,
			WorkoutOccurrenceOrigin origin,
			String generationKey,
			LocalDate originalScheduledDate,
			boolean manuallyRescheduled,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.trainingPlanId = trainingPlanId;
		this.workoutDayId = workoutDayId;
		this.athleteId = athleteId;
		this.scheduledDate = scheduledDate;
		this.plannedStartTime = plannedStartTime;
		this.startedAt = startedAt;
		this.completedAt = completedAt;
		this.status = status;
		this.athleteNotes = athleteNotes;
		this.origin = origin;
		this.generationKey = generationKey;
		this.originalScheduledDate = originalScheduledDate;
		this.manuallyRescheduled = manuallyRescheduled;
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

	UUID getTrainingPlanId() {
		return trainingPlanId;
	}

	UUID getWorkoutDayId() {
		return workoutDayId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	LocalDate getScheduledDate() {
		return scheduledDate;
	}

	LocalTime getPlannedStartTime() {
		return plannedStartTime;
	}

	Instant getStartedAt() {
		return startedAt;
	}

	Instant getCompletedAt() {
		return completedAt;
	}

	WorkoutOccurrenceStatus getStatus() {
		return status;
	}

	String getAthleteNotes() {
		return athleteNotes;
	}

	WorkoutOccurrenceOrigin getOrigin() {
		return origin;
	}

	String getGenerationKey() {
		return generationKey;
	}

	LocalDate getOriginalScheduledDate() {
		return originalScheduledDate;
	}

	boolean isManuallyRescheduled() {
		return manuallyRescheduled;
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

	void setScheduledDate(LocalDate scheduledDate) {
		this.scheduledDate = scheduledDate;
	}

	void setPlannedStartTime(LocalTime plannedStartTime) {
		this.plannedStartTime = plannedStartTime;
	}

	void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}

	void setStatus(WorkoutOccurrenceStatus status) {
		this.status = status;
	}

	void setAthleteNotes(String athleteNotes) {
		this.athleteNotes = athleteNotes;
	}

	void setOriginalScheduledDate(LocalDate originalScheduledDate) {
		this.originalScheduledDate = originalScheduledDate;
	}

	void setManuallyRescheduled(boolean manuallyRescheduled) {
		this.manuallyRescheduled = manuallyRescheduled;
	}

	void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

}
