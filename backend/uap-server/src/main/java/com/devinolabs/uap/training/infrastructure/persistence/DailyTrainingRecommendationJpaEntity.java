package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationReasonCode;
import com.devinolabs.uap.training.domain.TrainingRecommendationStatus;

@Entity
@Table(name = "daily_training_recommendations")
class DailyTrainingRecommendationJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "state_date", nullable = false, updatable = false)
	private LocalDate stateDate;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "daily_readiness_assessment_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID dailyReadinessAssessmentId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "daily_athlete_state_snapshot_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID dailyAthleteStateSnapshotId;

	@Column(name = "daily_athlete_state_snapshot_version", nullable = false, updatable = false)
	private int dailyAthleteStateSnapshotVersion;

	@Enumerated(EnumType.STRING)
	@Column(name = "recommendation_algorithm_version", nullable = false, updatable = false, length = 64)
	private TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion;

	@Enumerated(EnumType.STRING)
	@Column(name = "overall_action", nullable = false, updatable = false, length = 40)
	private TrainingRecommendationAction overallAction;

	@Enumerated(EnumType.STRING)
	@Column(name = "recommendation_status", nullable = false, updatable = false, length = 32)
	private TrainingRecommendationStatus recommendationStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "primary_reason_code", nullable = false, updatable = false, length = 64)
	private TrainingRecommendationReasonCode primaryReasonCode;

	@Column(name = "scheduled_training_present", nullable = false, updatable = false)
	private boolean scheduledTrainingPresent;

	@Column(name = "scheduled_occurrence_count", nullable = false, updatable = false)
	private int scheduledOccurrenceCount;

	@Column(name = "modifiable_scheduled_occurrence_count", nullable = false, updatable = false)
	private int modifiableScheduledOccurrenceCount;

	@Column(name = "adjustment_count", nullable = false, updatable = false)
	private int adjustmentCount;

	@Column(name = "limiting_dimension_count", nullable = false, updatable = false)
	private int limitingDimensionCount;

	@Column(name = "generated_at", nullable = false, updatable = false)
	private Instant generatedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("orderIndex ASC")
	@BatchSize(size = 32)
	private List<DailyTrainingRecommendationAdjustmentJpaEntity> adjustments = new ArrayList<>();

	@OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("orderIndex ASC")
	@BatchSize(size = 32)
	private List<DailyTrainingRecommendationOccurrenceJpaEntity> occurrences = new ArrayList<>();

	@Transient
	private boolean isNew = true;

	protected DailyTrainingRecommendationJpaEntity() {
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

	UUID getAthleteId() { return athleteId; }
	LocalDate getStateDate() { return stateDate; }
	UUID getDailyReadinessAssessmentId() { return dailyReadinessAssessmentId; }
	UUID getDailyAthleteStateSnapshotId() { return dailyAthleteStateSnapshotId; }
	int getDailyAthleteStateSnapshotVersion() { return dailyAthleteStateSnapshotVersion; }
	TrainingRecommendationAlgorithmVersion getRecommendationAlgorithmVersion() { return recommendationAlgorithmVersion; }
	TrainingRecommendationAction getOverallAction() { return overallAction; }
	TrainingRecommendationStatus getRecommendationStatus() { return recommendationStatus; }
	TrainingRecommendationReasonCode getPrimaryReasonCode() { return primaryReasonCode; }
	boolean isScheduledTrainingPresent() { return scheduledTrainingPresent; }
	int getScheduledOccurrenceCount() { return scheduledOccurrenceCount; }
	int getModifiableScheduledOccurrenceCount() { return modifiableScheduledOccurrenceCount; }
	int getAdjustmentCount() { return adjustmentCount; }
	int getLimitingDimensionCount() { return limitingDimensionCount; }
	Instant getGeneratedAt() { return generatedAt; }
	Instant getCreatedAt() { return createdAt; }
	List<DailyTrainingRecommendationAdjustmentJpaEntity> getAdjustments() { return adjustments; }
	List<DailyTrainingRecommendationOccurrenceJpaEntity> getOccurrences() { return occurrences; }

	static DailyTrainingRecommendationJpaEntity createNew(
			UUID id,
			UUID athleteId,
			LocalDate stateDate,
			UUID dailyReadinessAssessmentId,
			UUID dailyAthleteStateSnapshotId,
			int dailyAthleteStateSnapshotVersion,
			TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
			TrainingRecommendationAction overallAction,
			TrainingRecommendationStatus recommendationStatus,
			TrainingRecommendationReasonCode primaryReasonCode,
			boolean scheduledTrainingPresent,
			int scheduledOccurrenceCount,
			int modifiableScheduledOccurrenceCount,
			int adjustmentCount,
			int limitingDimensionCount,
			Instant generatedAt,
			Instant createdAt) {
		DailyTrainingRecommendationJpaEntity entity = new DailyTrainingRecommendationJpaEntity();
		entity.id = id;
		entity.athleteId = athleteId;
		entity.stateDate = stateDate;
		entity.dailyReadinessAssessmentId = dailyReadinessAssessmentId;
		entity.dailyAthleteStateSnapshotId = dailyAthleteStateSnapshotId;
		entity.dailyAthleteStateSnapshotVersion = dailyAthleteStateSnapshotVersion;
		entity.recommendationAlgorithmVersion = recommendationAlgorithmVersion;
		entity.overallAction = overallAction;
		entity.recommendationStatus = recommendationStatus;
		entity.primaryReasonCode = primaryReasonCode;
		entity.scheduledTrainingPresent = scheduledTrainingPresent;
		entity.scheduledOccurrenceCount = scheduledOccurrenceCount;
		entity.modifiableScheduledOccurrenceCount = modifiableScheduledOccurrenceCount;
		entity.adjustmentCount = adjustmentCount;
		entity.limitingDimensionCount = limitingDimensionCount;
		entity.generatedAt = generatedAt;
		entity.createdAt = createdAt;
		entity.isNew = true;
		return entity;
	}

}
