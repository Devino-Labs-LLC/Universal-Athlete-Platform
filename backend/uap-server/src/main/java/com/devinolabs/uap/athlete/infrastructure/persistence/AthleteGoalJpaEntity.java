package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.math.BigDecimal;
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

import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalTargetUnit;
import com.devinolabs.uap.athlete.domain.GoalType;

@Entity
@Table(name = "athlete_goals")
class AthleteGoalJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "goal_type", nullable = false, length = 50, updatable = false)
	private GoalType goalType;

	@Column(name = "custom_goal_name", length = 120, updatable = false)
	private String customGoalName;

	@Column(name = "title", nullable = false, length = 160)
	private String title;

	@Column(name = "normalized_title", nullable = false, length = 160)
	private String normalizedTitle;

	@Column(name = "description", length = 1000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(name = "priority", nullable = false, length = 20)
	private GoalPriority priority;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private GoalStatus status;

	@Column(name = "target_value", precision = 12, scale = 3)
	private BigDecimal targetValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_unit", length = 30)
	private GoalTargetUnit targetUnit;

	@Column(name = "custom_target_unit", length = 60)
	private String customTargetUnit;

	@Column(name = "target_date")
	private LocalDate targetDate;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_sport_id", columnDefinition = "BINARY(16)")
	private UUID athleteSportId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected AthleteGoalJpaEntity() {
	}

	AthleteGoalJpaEntity(
			UUID id,
			UUID athleteId,
			GoalType goalType,
			String customGoalName,
			String title,
			String normalizedTitle,
			String description,
			GoalPriority priority,
			GoalStatus status,
			BigDecimal targetValue,
			GoalTargetUnit targetUnit,
			String customTargetUnit,
			LocalDate targetDate,
			UUID athleteSportId,
			Instant createdAt,
			Instant updatedAt,
			Instant completedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.athleteId = athleteId;
		this.goalType = goalType;
		this.customGoalName = customGoalName;
		this.title = title;
		this.normalizedTitle = normalizedTitle;
		this.description = description;
		this.priority = priority;
		this.status = status;
		this.targetValue = targetValue;
		this.targetUnit = targetUnit;
		this.customTargetUnit = customTargetUnit;
		this.targetDate = targetDate;
		this.athleteSportId = athleteSportId;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.completedAt = completedAt;
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

	GoalType getGoalType() {
		return goalType;
	}

	String getCustomGoalName() {
		return customGoalName;
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

	GoalPriority getPriority() {
		return priority;
	}

	GoalStatus getStatus() {
		return status;
	}

	BigDecimal getTargetValue() {
		return targetValue;
	}

	GoalTargetUnit getTargetUnit() {
		return targetUnit;
	}

	String getCustomTargetUnit() {
		return customTargetUnit;
	}

	LocalDate getTargetDate() {
		return targetDate;
	}

	UUID getAthleteSportId() {
		return athleteSportId;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	Instant getCompletedAt() {
		return completedAt;
	}

	long getVersion() {
		return version;
	}

}
