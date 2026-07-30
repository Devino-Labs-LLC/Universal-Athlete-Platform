package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.Instant;
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

import com.devinolabs.uap.training.domain.WorkoutDayStatus;

@Entity
@Table(name = "workout_days")
class WorkoutDayJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "training_plan_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID trainingPlanId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "training_environment_override_id", columnDefinition = "BINARY(16)")
	private UUID trainingEnvironmentOverrideId;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "title", nullable = false, length = 160)
	private String title;

	@Column(name = "normalized_title", nullable = false, length = 160)
	private String normalizedTitle;

	@Column(name = "description", length = 2000)
	private String description;

	@Column(name = "plan_week_number")
	private Integer planWeekNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "scheduled_day_of_week", nullable = false, length = 16)
	private DayOfWeek scheduledDayOfWeek;

	@Column(name = "planned_start_time")
	private LocalTime plannedStartTime;

	@Column(name = "expected_duration_minutes")
	private Integer expectedDurationMinutes;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private WorkoutDayStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected WorkoutDayJpaEntity() {
	}

	WorkoutDayJpaEntity(
			UUID id,
			UUID trainingPlanId,
			UUID athleteId,
			UUID trainingEnvironmentOverrideId,
			int displayOrder,
			String title,
			String normalizedTitle,
			String description,
			Integer planWeekNumber,
			DayOfWeek scheduledDayOfWeek,
			LocalTime plannedStartTime,
			Integer expectedDurationMinutes,
			WorkoutDayStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.trainingPlanId = trainingPlanId;
		this.athleteId = athleteId;
		this.trainingEnvironmentOverrideId = trainingEnvironmentOverrideId;
		this.displayOrder = displayOrder;
		this.title = title;
		this.normalizedTitle = normalizedTitle;
		this.description = description;
		this.planWeekNumber = planWeekNumber;
		this.scheduledDayOfWeek = scheduledDayOfWeek;
		this.plannedStartTime = plannedStartTime;
		this.expectedDurationMinutes = expectedDurationMinutes;
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

	UUID getTrainingPlanId() {
		return trainingPlanId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	UUID getTrainingEnvironmentOverrideId() {
		return trainingEnvironmentOverrideId;
	}

	void setTrainingEnvironmentOverrideId(UUID trainingEnvironmentOverrideId) {
		this.trainingEnvironmentOverrideId = trainingEnvironmentOverrideId;
	}

	int getDisplayOrder() {
		return displayOrder;
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

	Integer getPlanWeekNumber() {
		return planWeekNumber;
	}

	DayOfWeek getScheduledDayOfWeek() {
		return scheduledDayOfWeek;
	}

	LocalTime getPlannedStartTime() {
		return plannedStartTime;
	}

	Integer getExpectedDurationMinutes() {
		return expectedDurationMinutes;
	}

	WorkoutDayStatus getStatus() {
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
