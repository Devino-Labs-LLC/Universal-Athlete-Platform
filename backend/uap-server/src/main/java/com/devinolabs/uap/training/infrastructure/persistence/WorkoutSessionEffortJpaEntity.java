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

import com.devinolabs.uap.training.domain.SessionDurationSource;
import com.devinolabs.uap.training.domain.SessionEffortSource;

@Entity
@Table(name = "workout_session_efforts")
class WorkoutSessionEffortJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "training_plan_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID trainingPlanId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_day_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutDayId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_occurrence_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutOccurrenceId;

	@Column(name = "session_rpe", nullable = false, precision = 3, scale = 1)
	private BigDecimal sessionRpe;

	@Column(name = "session_duration_minutes")
	private Integer sessionDurationMinutes;

	@Enumerated(EnumType.STRING)
	@Column(name = "duration_source", nullable = false, length = 32)
	private SessionDurationSource durationSource;

	@Column(name = "perceived_notes", length = 1000)
	private String perceivedNotes;

	@Column(name = "submitted_at", nullable = false)
	private Instant submittedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "effort_source", nullable = false, length = 32)
	private SessionEffortSource effortSource;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected WorkoutSessionEffortJpaEntity() {
	}

	@Override
	public UUID getId() { return id; }
	@Override public boolean isNew() { return isNew; }
	@PostLoad @PostPersist void markNotNew() { this.isNew = false; }

	UUID getAthleteId() { return athleteId; }
	UUID getTrainingPlanId() { return trainingPlanId; }
	UUID getWorkoutDayId() { return workoutDayId; }
	UUID getWorkoutOccurrenceId() { return workoutOccurrenceId; }
	BigDecimal getSessionRpe() { return sessionRpe; }
	Integer getSessionDurationMinutes() { return sessionDurationMinutes; }
	SessionDurationSource getDurationSource() { return durationSource; }
	String getPerceivedNotes() { return perceivedNotes; }
	Instant getSubmittedAt() { return submittedAt; }
	SessionEffortSource getEffortSource() { return effortSource; }
	Instant getCreatedAt() { return createdAt; }
	Instant getUpdatedAt() { return updatedAt; }
	long getVersion() { return version; }

	void setSessionRpe(BigDecimal sessionRpe) { this.sessionRpe = sessionRpe; }
	void setSessionDurationMinutes(Integer sessionDurationMinutes) { this.sessionDurationMinutes = sessionDurationMinutes; }
	void setDurationSource(SessionDurationSource durationSource) { this.durationSource = durationSource; }
	void setPerceivedNotes(String perceivedNotes) { this.perceivedNotes = perceivedNotes; }
	void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
	void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

	static WorkoutSessionEffortJpaEntity fromDomain(com.devinolabs.uap.training.domain.WorkoutSessionEffort effort, boolean isNew) {
		WorkoutSessionEffortJpaEntity entity = new WorkoutSessionEffortJpaEntity();
		entity.id = effort.id().value();
		entity.athleteId = effort.athleteId().value();
		entity.trainingPlanId = effort.trainingPlanId().value();
		entity.workoutDayId = effort.workoutDayId().value();
		entity.workoutOccurrenceId = effort.workoutOccurrenceId().value();
		entity.sessionRpe = effort.sessionRpe().value();
		entity.sessionDurationMinutes = effort.sessionDurationMinutes();
		entity.durationSource = effort.durationSource();
		entity.perceivedNotes = effort.perceivedNotes();
		entity.submittedAt = effort.submittedAt();
		entity.effortSource = effort.effortSource();
		entity.createdAt = effort.createdAt();
		entity.updatedAt = effort.updatedAt();
		entity.version = effort.version();
		entity.isNew = isNew;
		return entity;
	}
}
