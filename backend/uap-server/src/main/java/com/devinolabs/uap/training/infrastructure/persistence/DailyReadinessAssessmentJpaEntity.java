package com.devinolabs.uap.training.infrastructure.persistence;

import java.math.BigDecimal;
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

import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDataSufficiency;
import com.devinolabs.uap.training.domain.ReadinessReasonCode;

@Entity
@Table(name = "daily_readiness_assessments")
class DailyReadinessAssessmentJpaEntity implements Persistable<UUID> {

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
	@Column(name = "daily_athlete_state_snapshot_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID dailyAthleteStateSnapshotId;

	@Column(name = "daily_athlete_state_snapshot_version", nullable = false, updatable = false)
	private int dailyAthleteStateSnapshotVersion;

	@Enumerated(EnumType.STRING)
	@Column(name = "algorithm_version", nullable = false, updatable = false, length = 40)
	private ReadinessAlgorithmVersion algorithmVersion;

	@Column(name = "readiness_score", updatable = false, precision = 5, scale = 2)
	private BigDecimal readinessScore;

	@Enumerated(EnumType.STRING)
	@Column(name = "readiness_band", nullable = false, updatable = false, length = 32)
	private ReadinessBand readinessBand;

	@Enumerated(EnumType.STRING)
	@Column(name = "data_sufficiency", nullable = false, updatable = false, length = 16)
	private ReadinessDataSufficiency dataSufficiency;

	@Enumerated(EnumType.STRING)
	@Column(name = "summary_reason_code", nullable = false, updatable = false, length = 64)
	private ReadinessReasonCode summaryReasonCode;

	@Column(name = "limiting_dimension_count", nullable = false, updatable = false)
	private int limitingDimensionCount;

	@Column(name = "contributing_dimension_count", nullable = false, updatable = false)
	private int contributingDimensionCount;

	@Column(name = "assessed_at", nullable = false, updatable = false)
	private Instant assessedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@BatchSize(size = 32)
	private List<DailyReadinessDimensionContributionJpaEntity> contributions = new ArrayList<>();

	@OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("rankOrder ASC")
	@BatchSize(size = 16)
	private List<DailyReadinessLimitingDimensionJpaEntity> limitingDimensions = new ArrayList<>();

	@OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("rankOrder ASC")
	@BatchSize(size = 16)
	private List<DailyReadinessStrongestDimensionJpaEntity> strongestDimensions = new ArrayList<>();

	@Transient
	private boolean isNew = true;

	protected DailyReadinessAssessmentJpaEntity() {
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
	UUID getDailyAthleteStateSnapshotId() { return dailyAthleteStateSnapshotId; }
	int getDailyAthleteStateSnapshotVersion() { return dailyAthleteStateSnapshotVersion; }
	ReadinessAlgorithmVersion getAlgorithmVersion() { return algorithmVersion; }
	BigDecimal getReadinessScore() { return readinessScore; }
	ReadinessBand getReadinessBand() { return readinessBand; }
	ReadinessDataSufficiency getDataSufficiency() { return dataSufficiency; }
	ReadinessReasonCode getSummaryReasonCode() { return summaryReasonCode; }
	int getLimitingDimensionCount() { return limitingDimensionCount; }
	int getContributingDimensionCount() { return contributingDimensionCount; }
	Instant getAssessedAt() { return assessedAt; }
	Instant getCreatedAt() { return createdAt; }
	List<DailyReadinessDimensionContributionJpaEntity> getContributions() { return contributions; }
	List<DailyReadinessLimitingDimensionJpaEntity> getLimitingDimensions() { return limitingDimensions; }
	List<DailyReadinessStrongestDimensionJpaEntity> getStrongestDimensions() { return strongestDimensions; }

	static DailyReadinessAssessmentJpaEntity createNew(
			UUID id,
			UUID athleteId,
			LocalDate stateDate,
			UUID dailyAthleteStateSnapshotId,
			int dailyAthleteStateSnapshotVersion,
			ReadinessAlgorithmVersion algorithmVersion,
			BigDecimal readinessScore,
			ReadinessBand readinessBand,
			ReadinessDataSufficiency dataSufficiency,
			ReadinessReasonCode summaryReasonCode,
			int limitingDimensionCount,
			int contributingDimensionCount,
			Instant assessedAt,
			Instant createdAt) {
		DailyReadinessAssessmentJpaEntity entity = new DailyReadinessAssessmentJpaEntity();
		entity.id = id;
		entity.athleteId = athleteId;
		entity.stateDate = stateDate;
		entity.dailyAthleteStateSnapshotId = dailyAthleteStateSnapshotId;
		entity.dailyAthleteStateSnapshotVersion = dailyAthleteStateSnapshotVersion;
		entity.algorithmVersion = algorithmVersion;
		entity.readinessScore = readinessScore;
		entity.readinessBand = readinessBand;
		entity.dataSufficiency = dataSufficiency;
		entity.summaryReasonCode = summaryReasonCode;
		entity.limitingDimensionCount = limitingDimensionCount;
		entity.contributingDimensionCount = contributingDimensionCount;
		entity.assessedAt = assessedAt;
		entity.createdAt = createdAt;
		entity.isNew = true;
		return entity;
	}

}
