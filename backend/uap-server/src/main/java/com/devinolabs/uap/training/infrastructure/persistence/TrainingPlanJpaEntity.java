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

import com.devinolabs.uap.training.domain.TrainingPlanRecurrenceMode;
import com.devinolabs.uap.training.domain.TrainingPlanScheduleStatus;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;

@Entity
@Table(name = "training_plans")
class TrainingPlanJpaEntity implements Persistable<UUID> {

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

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "default_training_environment_id", columnDefinition = "BINARY(16)")
	private UUID defaultTrainingEnvironmentId;

	@Column(name = "name", nullable = false, length = 160)
	private String name;

	@Column(name = "normalized_name", nullable = false, length = 160)
	private String normalizedName;

	@Column(name = "description", length = 2000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "plan_type", nullable = false, length = 40, updatable = false)
	private TrainingPlanType planType;

	@Column(name = "custom_type_name", length = 120, updatable = false)
	private String customTypeName;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private TrainingPlanStatus status;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "schedule_start_date")
	private LocalDate scheduleStartDate;

	@Column(name = "schedule_end_date")
	private LocalDate scheduleEndDate;

	@Column(name = "schedule_timezone", length = 64)
	private String scheduleTimezone;

	@Enumerated(EnumType.STRING)
	@Column(name = "schedule_status", nullable = false, length = 20)
	private TrainingPlanScheduleStatus scheduleStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "recurrence_mode", length = 20)
	private TrainingPlanRecurrenceMode recurrenceMode;

	@Column(name = "schedule_generated_through")
	private LocalDate scheduleGeneratedThrough;

	@Column(name = "schedule_activated_at")
	private Instant scheduleActivatedAt;

	@Column(name = "schedule_paused_at")
	private Instant schedulePausedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected TrainingPlanJpaEntity() {
	}

	TrainingPlanJpaEntity(
			UUID id,
			UUID athleteId,
			UUID athleteSportId,
			UUID athleteGoalId,
			UUID defaultTrainingEnvironmentId,
			String name,
			String normalizedName,
			String description,
			TrainingPlanType planType,
			String customTypeName,
			TrainingPlanStatus status,
			LocalDate startDate,
			LocalDate endDate,
			LocalDate scheduleStartDate,
			LocalDate scheduleEndDate,
			String scheduleTimezone,
			TrainingPlanScheduleStatus scheduleStatus,
			TrainingPlanRecurrenceMode recurrenceMode,
			LocalDate scheduleGeneratedThrough,
			Instant scheduleActivatedAt,
			Instant schedulePausedAt,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.athleteId = athleteId;
		this.athleteSportId = athleteSportId;
		this.athleteGoalId = athleteGoalId;
		this.defaultTrainingEnvironmentId = defaultTrainingEnvironmentId;
		this.name = name;
		this.normalizedName = normalizedName;
		this.description = description;
		this.planType = planType;
		this.customTypeName = customTypeName;
		this.status = status;
		this.startDate = startDate;
		this.endDate = endDate;
		this.scheduleStartDate = scheduleStartDate;
		this.scheduleEndDate = scheduleEndDate;
		this.scheduleTimezone = scheduleTimezone;
		this.scheduleStatus = scheduleStatus;
		this.recurrenceMode = recurrenceMode;
		this.scheduleGeneratedThrough = scheduleGeneratedThrough;
		this.scheduleActivatedAt = scheduleActivatedAt;
		this.schedulePausedAt = schedulePausedAt;
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

	UUID getDefaultTrainingEnvironmentId() {
		return defaultTrainingEnvironmentId;
	}

	void setDefaultTrainingEnvironmentId(UUID defaultTrainingEnvironmentId) {
		this.defaultTrainingEnvironmentId = defaultTrainingEnvironmentId;
	}

	String getName() {
		return name;
	}

	String getNormalizedName() {
		return normalizedName;
	}

	String getDescription() {
		return description;
	}

	TrainingPlanType getPlanType() {
		return planType;
	}

	String getCustomTypeName() {
		return customTypeName;
	}

	TrainingPlanStatus getStatus() {
		return status;
	}

	LocalDate getStartDate() {
		return startDate;
	}

	LocalDate getEndDate() {
		return endDate;
	}

	LocalDate getScheduleStartDate() {
		return scheduleStartDate;
	}

	LocalDate getScheduleEndDate() {
		return scheduleEndDate;
	}

	String getScheduleTimezone() {
		return scheduleTimezone;
	}

	TrainingPlanScheduleStatus getScheduleStatus() {
		return scheduleStatus;
	}

	TrainingPlanRecurrenceMode getRecurrenceMode() {
		return recurrenceMode;
	}

	LocalDate getScheduleGeneratedThrough() {
		return scheduleGeneratedThrough;
	}

	Instant getScheduleActivatedAt() {
		return scheduleActivatedAt;
	}

	Instant getSchedulePausedAt() {
		return schedulePausedAt;
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
