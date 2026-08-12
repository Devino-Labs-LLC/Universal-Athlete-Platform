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

import com.devinolabs.uap.training.domain.DailyAthleteStateCompleteness;
import com.devinolabs.uap.training.domain.DailyAthleteStateGenerationReason;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsCalculationVersion;

@Entity
@Table(name = "daily_athlete_state_snapshots")
class DailyAthleteStateSnapshotJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "state_date", nullable = false, updatable = false)
	private LocalDate stateDate;

	@Column(name = "snapshot_version", nullable = false, updatable = false)
	private int snapshotVersion;

	@Column(name = "current_snapshot", nullable = false)
	private boolean currentSnapshot;

	@Column(name = "source_fingerprint", nullable = false, updatable = false, length = 64)
	private String sourceFingerprint;

	@Enumerated(EnumType.STRING)
	@Column(name = "generation_reason", nullable = false, updatable = false, length = 40)
	private DailyAthleteStateGenerationReason generationReason;

	@Column(name = "generated_at", nullable = false, updatable = false)
	private Instant generatedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "completeness", nullable = false, updatable = false, length = 16)
	private DailyAthleteStateCompleteness completeness;

	@Column(name = "baseline_window_days", nullable = false, updatable = false)
	private int baselineWindowDays;

	@Enumerated(EnumType.STRING)
	@Column(name = "recovery_analytics_calculation_version", nullable = false, updatable = false, length = 40)
	private RecoveryAnalyticsCalculationVersion recoveryAnalyticsCalculationVersion;

	@Column(name = "check_in_present", nullable = false, updatable = false)
	private boolean checkInPresent;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "recovery_check_in_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID recoveryCheckInId;

	@Column(name = "recovery_check_in_version", updatable = false)
	private Long recoveryCheckInVersion;

	@Column(name = "sleep_duration_minutes", updatable = false)
	private Integer sleepDurationMinutes;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "sleep_quality", updatable = false)
	private Integer sleepQuality;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "fatigue", updatable = false)
	private Integer fatigue;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "muscle_soreness", updatable = false)
	private Integer muscleSoreness;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "stress", updatable = false)
	private Integer stress;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "mood", updatable = false)
	private Integer mood;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "motivation", updatable = false)
	private Integer motivation;

	@Column(name = "check_in_submitted_at", updatable = false)
	private Instant checkInSubmittedAt;

	@Column(name = "check_in_last_updated_at", updatable = false)
	private Instant checkInLastUpdatedAt;

	@Column(name = "occurrence_count", nullable = false, updatable = false)
	private long occurrenceCount;

	@Column(name = "completed_occurrence_count", nullable = false, updatable = false)
	private long completedOccurrenceCount;

	@Column(name = "rated_occurrence_count", nullable = false, updatable = false)
	private long ratedOccurrenceCount;

	@Column(name = "unrated_occurrence_count", nullable = false, updatable = false)
	private long unratedOccurrenceCount;

	@Column(name = "completed_exercise_count", nullable = false, updatable = false)
	private long completedExerciseCount;

	@Column(name = "completed_set_count", nullable = false, updatable = false)
	private long completedSetCount;

	@Column(name = "completed_repetition_count", nullable = false, updatable = false)
	private long completedRepetitionCount;

	@Column(name = "total_volume_kilograms", nullable = false, updatable = false, precision = 18, scale = 3)
	private BigDecimal totalVolumeKilograms;

	@Column(name = "total_duration_seconds", nullable = false, updatable = false)
	private long totalDurationSeconds;

	@Column(name = "total_distance_meters", nullable = false, updatable = false, precision = 18, scale = 3)
	private BigDecimal totalDistanceMeters;

	@Column(name = "total_session_rpe_load", updatable = false, precision = 12, scale = 2)
	private BigDecimal totalSessionRpeLoad;

	@Column(name = "average_session_rpe", updatable = false, precision = 3, scale = 1)
	private BigDecimal averageSessionRpe;

	@Column(name = "total_session_duration_minutes", nullable = false, updatable = false)
	private long totalSessionDurationMinutes;

	@Column(name = "no_impact_exercise_count", nullable = false, updatable = false)
	private long noImpactExerciseCount;

	@Column(name = "low_impact_exercise_count", nullable = false, updatable = false)
	private long lowImpactExerciseCount;

	@Column(name = "moderate_impact_exercise_count", nullable = false, updatable = false)
	private long moderateImpactExerciseCount;

	@Column(name = "high_impact_exercise_count", nullable = false, updatable = false)
	private long highImpactExerciseCount;

	@Column(name = "scheduled_occurrence_count", nullable = false, updatable = false)
	private long scheduledOccurrenceCount;

	@Column(name = "scheduled_workout_count", nullable = false, updatable = false)
	private long scheduledWorkoutCount;

	@Column(name = "completed_scheduled_count", nullable = false, updatable = false)
	private long completedScheduledCount;

	@Column(name = "skipped_scheduled_count", nullable = false, updatable = false)
	private long skippedScheduledCount;

	@Column(name = "cancelled_scheduled_count", nullable = false, updatable = false)
	private long cancelledScheduledCount;

	@Column(name = "in_progress_scheduled_count", nullable = false, updatable = false)
	private long inProgressScheduledCount;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@BatchSize(size = 32)
	private List<DailyAthleteStateRecoveryMetricJpaEntity> recoveryMetrics = new ArrayList<>();

	@OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("orderIndex ASC")
	@BatchSize(size = 32)
	private List<DailyAthleteStateDiscomfortJpaEntity> discomfort = new ArrayList<>();

	@OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@BatchSize(size = 32)
	private List<DailyAthleteStateCategorySummaryJpaEntity> categories = new ArrayList<>();

	@OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@BatchSize(size = 32)
	private List<DailyAthleteStateMovementSummaryJpaEntity> movements = new ArrayList<>();

	@OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("orderIndex ASC")
	@BatchSize(size = 32)
	private List<DailyAthleteStateScheduledOccurrenceJpaEntity> scheduledOccurrences = new ArrayList<>();

	@Transient
	private boolean isNew = true;

	protected DailyAthleteStateSnapshotJpaEntity() {
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

	void markNew() {
		this.isNew = true;
	}

	void setCurrentSnapshot(boolean currentSnapshot) {
		this.currentSnapshot = currentSnapshot;
	}

	boolean isCurrentSnapshot() {
		return currentSnapshot;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	LocalDate getStateDate() {
		return stateDate;
	}

	int getSnapshotVersion() {
		return snapshotVersion;
	}

	String getSourceFingerprint() {
		return sourceFingerprint;
	}

	DailyAthleteStateGenerationReason getGenerationReason() {
		return generationReason;
	}

	Instant getGeneratedAt() {
		return generatedAt;
	}

	DailyAthleteStateCompleteness getCompleteness() {
		return completeness;
	}

	int getBaselineWindowDays() {
		return baselineWindowDays;
	}

	RecoveryAnalyticsCalculationVersion getRecoveryAnalyticsCalculationVersion() {
		return recoveryAnalyticsCalculationVersion;
	}

	boolean isCheckInPresent() {
		return checkInPresent;
	}

	UUID getRecoveryCheckInId() {
		return recoveryCheckInId;
	}

	Long getRecoveryCheckInVersion() {
		return recoveryCheckInVersion;
	}

	Integer getSleepDurationMinutes() {
		return sleepDurationMinutes;
	}

	Integer getSleepQuality() {
		return sleepQuality;
	}

	Integer getFatigue() {
		return fatigue;
	}

	Integer getMuscleSoreness() {
		return muscleSoreness;
	}

	Integer getStress() {
		return stress;
	}

	Integer getMood() {
		return mood;
	}

	Integer getMotivation() {
		return motivation;
	}

	Instant getCheckInSubmittedAt() {
		return checkInSubmittedAt;
	}

	Instant getCheckInLastUpdatedAt() {
		return checkInLastUpdatedAt;
	}

	long getOccurrenceCount() {
		return occurrenceCount;
	}

	long getCompletedOccurrenceCount() {
		return completedOccurrenceCount;
	}

	long getRatedOccurrenceCount() {
		return ratedOccurrenceCount;
	}

	long getUnratedOccurrenceCount() {
		return unratedOccurrenceCount;
	}

	long getCompletedExerciseCount() {
		return completedExerciseCount;
	}

	long getCompletedSetCount() {
		return completedSetCount;
	}

	long getCompletedRepetitionCount() {
		return completedRepetitionCount;
	}

	BigDecimal getTotalVolumeKilograms() {
		return totalVolumeKilograms;
	}

	long getTotalDurationSeconds() {
		return totalDurationSeconds;
	}

	BigDecimal getTotalDistanceMeters() {
		return totalDistanceMeters;
	}

	BigDecimal getTotalSessionRpeLoad() {
		return totalSessionRpeLoad;
	}

	BigDecimal getAverageSessionRpe() {
		return averageSessionRpe;
	}

	long getTotalSessionDurationMinutes() {
		return totalSessionDurationMinutes;
	}

	long getNoImpactExerciseCount() {
		return noImpactExerciseCount;
	}

	long getLowImpactExerciseCount() {
		return lowImpactExerciseCount;
	}

	long getModerateImpactExerciseCount() {
		return moderateImpactExerciseCount;
	}

	long getHighImpactExerciseCount() {
		return highImpactExerciseCount;
	}

	long getScheduledOccurrenceCount() {
		return scheduledOccurrenceCount;
	}

	long getScheduledWorkoutCount() {
		return scheduledWorkoutCount;
	}

	long getCompletedScheduledCount() {
		return completedScheduledCount;
	}

	long getSkippedScheduledCount() {
		return skippedScheduledCount;
	}

	long getCancelledScheduledCount() {
		return cancelledScheduledCount;
	}

	long getInProgressScheduledCount() {
		return inProgressScheduledCount;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	List<DailyAthleteStateRecoveryMetricJpaEntity> getRecoveryMetrics() {
		return recoveryMetrics;
	}

	List<DailyAthleteStateDiscomfortJpaEntity> getDiscomfort() {
		return discomfort;
	}

	List<DailyAthleteStateCategorySummaryJpaEntity> getCategories() {
		return categories;
	}

	List<DailyAthleteStateMovementSummaryJpaEntity> getMovements() {
		return movements;
	}

	List<DailyAthleteStateScheduledOccurrenceJpaEntity> getScheduledOccurrences() {
		return scheduledOccurrences;
	}

	static DailyAthleteStateSnapshotJpaEntity createNew(
			UUID id,
			UUID athleteId,
			LocalDate stateDate,
			int snapshotVersion,
			boolean currentSnapshot,
			String sourceFingerprint,
			DailyAthleteStateGenerationReason generationReason,
			Instant generatedAt,
			DailyAthleteStateCompleteness completeness,
			int baselineWindowDays,
			RecoveryAnalyticsCalculationVersion recoveryAnalyticsCalculationVersion,
			boolean checkInPresent,
			UUID recoveryCheckInId,
			Long recoveryCheckInVersion,
			Integer sleepDurationMinutes,
			Integer sleepQuality,
			Integer fatigue,
			Integer muscleSoreness,
			Integer stress,
			Integer mood,
			Integer motivation,
			Instant checkInSubmittedAt,
			Instant checkInLastUpdatedAt,
			long occurrenceCount,
			long completedOccurrenceCount,
			long ratedOccurrenceCount,
			long unratedOccurrenceCount,
			long completedExerciseCount,
			long completedSetCount,
			long completedRepetitionCount,
			BigDecimal totalVolumeKilograms,
			long totalDurationSeconds,
			BigDecimal totalDistanceMeters,
			BigDecimal totalSessionRpeLoad,
			BigDecimal averageSessionRpe,
			long totalSessionDurationMinutes,
			long noImpactExerciseCount,
			long lowImpactExerciseCount,
			long moderateImpactExerciseCount,
			long highImpactExerciseCount,
			long scheduledOccurrenceCount,
			long scheduledWorkoutCount,
			long completedScheduledCount,
			long skippedScheduledCount,
			long cancelledScheduledCount,
			long inProgressScheduledCount,
			Instant createdAt) {
		DailyAthleteStateSnapshotJpaEntity entity = new DailyAthleteStateSnapshotJpaEntity();
		entity.id = id;
		entity.athleteId = athleteId;
		entity.stateDate = stateDate;
		entity.snapshotVersion = snapshotVersion;
		entity.currentSnapshot = currentSnapshot;
		entity.sourceFingerprint = sourceFingerprint;
		entity.generationReason = generationReason;
		entity.generatedAt = generatedAt;
		entity.completeness = completeness;
		entity.baselineWindowDays = baselineWindowDays;
		entity.recoveryAnalyticsCalculationVersion = recoveryAnalyticsCalculationVersion;
		entity.checkInPresent = checkInPresent;
		entity.recoveryCheckInId = recoveryCheckInId;
		entity.recoveryCheckInVersion = recoveryCheckInVersion;
		entity.sleepDurationMinutes = sleepDurationMinutes;
		entity.sleepQuality = sleepQuality;
		entity.fatigue = fatigue;
		entity.muscleSoreness = muscleSoreness;
		entity.stress = stress;
		entity.mood = mood;
		entity.motivation = motivation;
		entity.checkInSubmittedAt = checkInSubmittedAt;
		entity.checkInLastUpdatedAt = checkInLastUpdatedAt;
		entity.occurrenceCount = occurrenceCount;
		entity.completedOccurrenceCount = completedOccurrenceCount;
		entity.ratedOccurrenceCount = ratedOccurrenceCount;
		entity.unratedOccurrenceCount = unratedOccurrenceCount;
		entity.completedExerciseCount = completedExerciseCount;
		entity.completedSetCount = completedSetCount;
		entity.completedRepetitionCount = completedRepetitionCount;
		entity.totalVolumeKilograms = totalVolumeKilograms;
		entity.totalDurationSeconds = totalDurationSeconds;
		entity.totalDistanceMeters = totalDistanceMeters;
		entity.totalSessionRpeLoad = totalSessionRpeLoad;
		entity.averageSessionRpe = averageSessionRpe;
		entity.totalSessionDurationMinutes = totalSessionDurationMinutes;
		entity.noImpactExerciseCount = noImpactExerciseCount;
		entity.lowImpactExerciseCount = lowImpactExerciseCount;
		entity.moderateImpactExerciseCount = moderateImpactExerciseCount;
		entity.highImpactExerciseCount = highImpactExerciseCount;
		entity.scheduledOccurrenceCount = scheduledOccurrenceCount;
		entity.scheduledWorkoutCount = scheduledWorkoutCount;
		entity.completedScheduledCount = completedScheduledCount;
		entity.skippedScheduledCount = skippedScheduledCount;
		entity.cancelledScheduledCount = cancelledScheduledCount;
		entity.inProgressScheduledCount = inProgressScheduledCount;
		entity.createdAt = createdAt;
		entity.isNew = true;
		return entity;
	}

}
