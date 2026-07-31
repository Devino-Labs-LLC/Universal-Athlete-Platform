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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "workout_occurrence_load_summaries")
class WorkoutOccurrenceLoadSummaryJpaEntity implements Persistable<UUID> {

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

	@Column(name = "scheduled_date", nullable = false, updatable = false)
	private LocalDate scheduledDate;

	@Column(name = "session_rpe", precision = 3, scale = 1)
	private BigDecimal sessionRpe;

	@Column(name = "session_duration_minutes")
	private Integer sessionDurationMinutes;

	@Column(name = "session_rpe_load", precision = 12, scale = 2)
	private BigDecimal sessionRpeLoad;

	@Column(name = "prescribed_exercise_count", nullable = false)
	private long prescribedExerciseCount;

	@Column(name = "completed_exercise_count", nullable = false)
	private long completedExerciseCount;

	@Column(name = "substituted_exercise_count", nullable = false)
	private long substitutedExerciseCount;

	@Column(name = "completed_set_count", nullable = false)
	private long completedSetCount;

	@Column(name = "skipped_set_count", nullable = false)
	private long skippedSetCount;

	@Column(name = "completed_repetition_count", nullable = false)
	private long completedRepetitionCount;

	@Column(name = "total_volume_kilograms", nullable = false, precision = 18, scale = 3)
	private BigDecimal totalVolumeKilograms;

	@Column(name = "total_duration_seconds", nullable = false)
	private long totalDurationSeconds;

	@Column(name = "total_distance_meters", nullable = false, precision = 18, scale = 3)
	private BigDecimal totalDistanceMeters;

	@Column(name = "no_impact_exercise_count", nullable = false)
	private long noImpactExerciseCount;

	@Column(name = "low_impact_exercise_count", nullable = false)
	private long lowImpactExerciseCount;

	@Column(name = "moderate_impact_exercise_count", nullable = false)
	private long moderateImpactExerciseCount;

	@Column(name = "high_impact_exercise_count", nullable = false)
	private long highImpactExerciseCount;

	@Column(name = "calculated_at", nullable = false)
	private Instant calculatedAt;

	@Column(name = "source_updated_at", nullable = false)
	private Instant sourceUpdatedAt;

	@Column(name = "calculation_version", nullable = false, length = 32)
	private String calculationVersion;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@OneToMany(mappedBy = "summary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<WorkoutOccurrenceLoadCategorySummaryJpaEntity> categories = new ArrayList<>();

	@OneToMany(mappedBy = "summary", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<WorkoutOccurrenceLoadMovementSummaryJpaEntity> movements = new ArrayList<>();

	@Transient
	private boolean isNew = true;

	protected WorkoutOccurrenceLoadSummaryJpaEntity() {
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

	UUID getTrainingPlanId() {
		return trainingPlanId;
	}

	UUID getWorkoutDayId() {
		return workoutDayId;
	}

	UUID getWorkoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	LocalDate getScheduledDate() {
		return scheduledDate;
	}

	BigDecimal getSessionRpe() {
		return sessionRpe;
	}

	Integer getSessionDurationMinutes() {
		return sessionDurationMinutes;
	}

	BigDecimal getSessionRpeLoad() {
		return sessionRpeLoad;
	}

	long getPrescribedExerciseCount() {
		return prescribedExerciseCount;
	}

	long getCompletedExerciseCount() {
		return completedExerciseCount;
	}

	long getSubstitutedExerciseCount() {
		return substitutedExerciseCount;
	}

	long getCompletedSetCount() {
		return completedSetCount;
	}

	long getSkippedSetCount() {
		return skippedSetCount;
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

	Instant getCalculatedAt() {
		return calculatedAt;
	}

	Instant getSourceUpdatedAt() {
		return sourceUpdatedAt;
	}

	String getCalculationVersion() {
		return calculationVersion;
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

	List<WorkoutOccurrenceLoadCategorySummaryJpaEntity> getCategories() {
		return categories;
	}

	List<WorkoutOccurrenceLoadMovementSummaryJpaEntity> getMovements() {
		return movements;
	}

	void setCategories(List<WorkoutOccurrenceLoadCategorySummaryJpaEntity> categories) {
		this.categories.clear();
		this.categories.addAll(categories);
	}

	void setMovements(List<WorkoutOccurrenceLoadMovementSummaryJpaEntity> movements) {
		this.movements.clear();
		this.movements.addAll(movements);
	}

	void setId(UUID id) {
		this.id = id;
	}

	void setAthleteId(UUID athleteId) {
		this.athleteId = athleteId;
	}

	void setTrainingPlanId(UUID trainingPlanId) {
		this.trainingPlanId = trainingPlanId;
	}

	void setWorkoutDayId(UUID workoutDayId) {
		this.workoutDayId = workoutDayId;
	}

	void setWorkoutOccurrenceId(UUID workoutOccurrenceId) {
		this.workoutOccurrenceId = workoutOccurrenceId;
	}

	void setScheduledDate(LocalDate scheduledDate) {
		this.scheduledDate = scheduledDate;
	}

	void setSessionRpe(BigDecimal sessionRpe) {
		this.sessionRpe = sessionRpe;
	}

	void setSessionDurationMinutes(Integer sessionDurationMinutes) {
		this.sessionDurationMinutes = sessionDurationMinutes;
	}

	void setSessionRpeLoad(BigDecimal sessionRpeLoad) {
		this.sessionRpeLoad = sessionRpeLoad;
	}

	void setPrescribedExerciseCount(long prescribedExerciseCount) {
		this.prescribedExerciseCount = prescribedExerciseCount;
	}

	void setCompletedExerciseCount(long completedExerciseCount) {
		this.completedExerciseCount = completedExerciseCount;
	}

	void setSubstitutedExerciseCount(long substitutedExerciseCount) {
		this.substitutedExerciseCount = substitutedExerciseCount;
	}

	void setCompletedSetCount(long completedSetCount) {
		this.completedSetCount = completedSetCount;
	}

	void setSkippedSetCount(long skippedSetCount) {
		this.skippedSetCount = skippedSetCount;
	}

	void setCompletedRepetitionCount(long completedRepetitionCount) {
		this.completedRepetitionCount = completedRepetitionCount;
	}

	void setTotalVolumeKilograms(BigDecimal totalVolumeKilograms) {
		this.totalVolumeKilograms = totalVolumeKilograms;
	}

	void setTotalDurationSeconds(long totalDurationSeconds) {
		this.totalDurationSeconds = totalDurationSeconds;
	}

	void setTotalDistanceMeters(BigDecimal totalDistanceMeters) {
		this.totalDistanceMeters = totalDistanceMeters;
	}

	void setNoImpactExerciseCount(long noImpactExerciseCount) {
		this.noImpactExerciseCount = noImpactExerciseCount;
	}

	void setLowImpactExerciseCount(long lowImpactExerciseCount) {
		this.lowImpactExerciseCount = lowImpactExerciseCount;
	}

	void setModerateImpactExerciseCount(long moderateImpactExerciseCount) {
		this.moderateImpactExerciseCount = moderateImpactExerciseCount;
	}

	void setHighImpactExerciseCount(long highImpactExerciseCount) {
		this.highImpactExerciseCount = highImpactExerciseCount;
	}

	void setCalculatedAt(Instant calculatedAt) {
		this.calculatedAt = calculatedAt;
	}

	void setSourceUpdatedAt(Instant sourceUpdatedAt) {
		this.sourceUpdatedAt = sourceUpdatedAt;
	}

	void setCalculationVersion(String calculationVersion) {
		this.calculationVersion = calculationVersion;
	}

	void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	void setVersion(long version) {
		this.version = version;
	}

	void setNew(boolean isNew) {
		this.isNew = isNew;
	}

}
