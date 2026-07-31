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

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;

@Entity
@Table(name = "workout_exercise_executions")
class WorkoutExerciseExecutionJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_occurrence_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutOccurrenceId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "source_workout_exercise_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID sourceWorkoutExerciseId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "prescribed_exercise_definition_id", nullable = false, updatable = false,
			columnDefinition = "BINARY(16)")
	private UUID prescribedExerciseDefinitionId;

	@Column(name = "prescribed_exercise_name_snapshot", nullable = false, updatable = false, length = 160)
	private String prescribedExerciseNameSnapshot;

	/**
	 * Performed identity, the performance key and the substitution details are the only movement
	 * columns that stay writable: a substitution moves them together while the prescribed snapshot
	 * above records what the plan asked for.
	 */
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "performed_exercise_definition_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID performedExerciseDefinitionId;

	@Column(name = "performed_exercise_name_snapshot", nullable = false, length = 160)
	private String performedExerciseNameSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "performed_exercise_category_snapshot", nullable = false, length = 32)
	private com.devinolabs.uap.training.domain.ExerciseDefinitionCategory performedExerciseCategorySnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "performed_primary_movement_pattern_snapshot", nullable = false, length = 40)
	private com.devinolabs.uap.training.domain.MovementPattern performedPrimaryMovementPatternSnapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "performed_impact_level_snapshot", nullable = false, length = 32)
	private com.devinolabs.uap.training.domain.ImpactLevel performedImpactLevelSnapshot;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "exercise_performance_key", nullable = false, columnDefinition = "BINARY(16)")
	private UUID exercisePerformanceKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "substitution_reason", length = 32)
	private ExerciseSubstitutionReason substitutionReason;

	@Column(name = "substitution_notes", length = 2000)
	private String substitutionNotes;

	@Column(name = "substituted_at")
	private Instant substitutedAt;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "display_order", nullable = false, updatable = false)
	private int displayOrder;

	@Enumerated(EnumType.STRING)
	@Column(name = "exercise_category_snapshot", nullable = false, updatable = false, length = 32)
	private ExerciseCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "exercise_type_snapshot", nullable = false, updatable = false, length = 32)
	private ExerciseType type;

	@Column(name = "prescribed_sets", nullable = false, updatable = false)
	private Integer prescribedSets;

	@Column(name = "prescribed_minimum_reps", updatable = false)
	private Integer prescribedMinimumReps;

	@Column(name = "prescribed_maximum_reps", updatable = false)
	private Integer prescribedMaximumReps;

	@Column(name = "prescribed_target_weight", precision = 12, scale = 4, updatable = false)
	private BigDecimal prescribedTargetWeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "prescribed_weight_unit", length = 16, updatable = false)
	private WeightUnit prescribedWeightUnit;

	@Column(name = "prescribed_target_duration_seconds", updatable = false)
	private Integer prescribedTargetDurationSeconds;

	@Column(name = "prescribed_target_distance", precision = 12, scale = 4, updatable = false)
	private BigDecimal prescribedTargetDistance;

	@Enumerated(EnumType.STRING)
	@Column(name = "prescribed_distance_unit", length = 16, updatable = false)
	private DistanceUnit prescribedDistanceUnit;

	@Column(name = "prescribed_target_rest_seconds", updatable = false)
	private Integer prescribedTargetRestSeconds;

	@Column(name = "prescribed_target_rpe", updatable = false)
	private Integer prescribedTargetRpe;

	@Column(name = "prescribed_tempo", length = 40, updatable = false)
	private String prescribedTempo;

	@Column(name = "prescribed_coaching_notes", length = 2000, updatable = false)
	private String prescribedCoachingNotes;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private WorkoutExerciseExecutionStatus status;

	@Column(name = "actual_sets")
	private Integer actualSets;

	@Column(name = "actual_reps")
	private Integer actualReps;

	@Column(name = "actual_weight", precision = 12, scale = 4)
	private BigDecimal actualWeight;

	@Enumerated(EnumType.STRING)
	@Column(name = "actual_weight_unit", length = 16)
	private WeightUnit weightUnit;

	@Column(name = "actual_duration_seconds")
	private Integer actualDurationSeconds;

	@Column(name = "actual_distance", precision = 12, scale = 4)
	private BigDecimal actualDistance;

	@Enumerated(EnumType.STRING)
	@Column(name = "actual_distance_unit", length = 16)
	private DistanceUnit distanceUnit;

	@Column(name = "actual_rest_seconds")
	private Integer actualRestSeconds;

	@Column(name = "actual_rpe", precision = 12, scale = 2)
	private BigDecimal actualRpe;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "athlete_notes", length = 4000)
	private String athleteNotes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected WorkoutExerciseExecutionJpaEntity() {
	}

	WorkoutExerciseExecutionJpaEntity(
			UUID id,
			UUID workoutOccurrenceId,
			UUID sourceWorkoutExerciseId,
			UUID prescribedExerciseDefinitionId,
			String prescribedExerciseNameSnapshot,
			UUID performedExerciseDefinitionId,
			String performedExerciseNameSnapshot,
			com.devinolabs.uap.training.domain.ExerciseDefinitionCategory performedExerciseCategorySnapshot,
			com.devinolabs.uap.training.domain.MovementPattern performedPrimaryMovementPatternSnapshot,
			com.devinolabs.uap.training.domain.ImpactLevel performedImpactLevelSnapshot,
			UUID exercisePerformanceKey,
			ExerciseSubstitutionReason substitutionReason,
			String substitutionNotes,
			Instant substitutedAt,
			UUID athleteId,
			int displayOrder,
			ExerciseCategory category,
			ExerciseType type,
			Integer prescribedSets,
			Integer prescribedMinimumReps,
			Integer prescribedMaximumReps,
			BigDecimal prescribedTargetWeight,
			WeightUnit prescribedWeightUnit,
			Integer prescribedTargetDurationSeconds,
			BigDecimal prescribedTargetDistance,
			DistanceUnit prescribedDistanceUnit,
			Integer prescribedTargetRestSeconds,
			Integer prescribedTargetRpe,
			String prescribedTempo,
			String prescribedCoachingNotes,
			WorkoutExerciseExecutionStatus status,
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe,
			Instant startedAt,
			Instant completedAt,
			String athleteNotes,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.workoutOccurrenceId = workoutOccurrenceId;
		this.sourceWorkoutExerciseId = sourceWorkoutExerciseId;
		this.prescribedExerciseDefinitionId = prescribedExerciseDefinitionId;
		this.prescribedExerciseNameSnapshot = prescribedExerciseNameSnapshot;
		this.performedExerciseDefinitionId = performedExerciseDefinitionId;
		this.performedExerciseNameSnapshot = performedExerciseNameSnapshot;
		this.performedExerciseCategorySnapshot = performedExerciseCategorySnapshot;
		this.performedPrimaryMovementPatternSnapshot = performedPrimaryMovementPatternSnapshot;
		this.performedImpactLevelSnapshot = performedImpactLevelSnapshot;
		this.exercisePerformanceKey = exercisePerformanceKey;
		this.substitutionReason = substitutionReason;
		this.substitutionNotes = substitutionNotes;
		this.substitutedAt = substitutedAt;
		this.athleteId = athleteId;
		this.displayOrder = displayOrder;
		this.category = category;
		this.type = type;
		this.prescribedSets = prescribedSets;
		this.prescribedMinimumReps = prescribedMinimumReps;
		this.prescribedMaximumReps = prescribedMaximumReps;
		this.prescribedTargetWeight = prescribedTargetWeight;
		this.prescribedWeightUnit = prescribedWeightUnit;
		this.prescribedTargetDurationSeconds = prescribedTargetDurationSeconds;
		this.prescribedTargetDistance = prescribedTargetDistance;
		this.prescribedDistanceUnit = prescribedDistanceUnit;
		this.prescribedTargetRestSeconds = prescribedTargetRestSeconds;
		this.prescribedTargetRpe = prescribedTargetRpe;
		this.prescribedTempo = prescribedTempo;
		this.prescribedCoachingNotes = prescribedCoachingNotes;
		this.status = status;
		this.actualSets = actualSets;
		this.actualReps = actualReps;
		this.actualWeight = actualWeight;
		this.weightUnit = weightUnit;
		this.actualDurationSeconds = actualDurationSeconds;
		this.actualDistance = actualDistance;
		this.distanceUnit = distanceUnit;
		this.actualRestSeconds = actualRestSeconds;
		this.actualRpe = actualRpe;
		this.startedAt = startedAt;
		this.completedAt = completedAt;
		this.athleteNotes = athleteNotes;
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

	UUID getWorkoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	UUID getSourceWorkoutExerciseId() {
		return sourceWorkoutExerciseId;
	}

	UUID getPrescribedExerciseDefinitionId() {
		return prescribedExerciseDefinitionId;
	}

	String getPrescribedExerciseNameSnapshot() {
		return prescribedExerciseNameSnapshot;
	}

	UUID getPerformedExerciseDefinitionId() {
		return performedExerciseDefinitionId;
	}

	String getPerformedExerciseNameSnapshot() {
		return performedExerciseNameSnapshot;
	}

	UUID getExercisePerformanceKey() {
		return exercisePerformanceKey;
	}

	ExerciseSubstitutionReason getSubstitutionReason() {
		return substitutionReason;
	}

	String getSubstitutionNotes() {
		return substitutionNotes;
	}

	Instant getSubstitutedAt() {
		return substitutedAt;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	int getDisplayOrder() {
		return displayOrder;
	}

	ExerciseCategory getCategory() {
		return category;
	}

	ExerciseType getType() {
		return type;
	}

	Integer getPrescribedSets() {
		return prescribedSets;
	}

	Integer getPrescribedMinimumReps() {
		return prescribedMinimumReps;
	}

	Integer getPrescribedMaximumReps() {
		return prescribedMaximumReps;
	}

	BigDecimal getPrescribedTargetWeight() {
		return prescribedTargetWeight;
	}

	WeightUnit getPrescribedWeightUnit() {
		return prescribedWeightUnit;
	}

	Integer getPrescribedTargetDurationSeconds() {
		return prescribedTargetDurationSeconds;
	}

	BigDecimal getPrescribedTargetDistance() {
		return prescribedTargetDistance;
	}

	DistanceUnit getPrescribedDistanceUnit() {
		return prescribedDistanceUnit;
	}

	Integer getPrescribedTargetRestSeconds() {
		return prescribedTargetRestSeconds;
	}

	Integer getPrescribedTargetRpe() {
		return prescribedTargetRpe;
	}

	String getPrescribedTempo() {
		return prescribedTempo;
	}

	String getPrescribedCoachingNotes() {
		return prescribedCoachingNotes;
	}

	WorkoutExerciseExecutionStatus getStatus() {
		return status;
	}

	Integer getActualSets() {
		return actualSets;
	}

	Integer getActualReps() {
		return actualReps;
	}

	BigDecimal getActualWeight() {
		return actualWeight;
	}

	WeightUnit getWeightUnit() {
		return weightUnit;
	}

	Integer getActualDurationSeconds() {
		return actualDurationSeconds;
	}

	BigDecimal getActualDistance() {
		return actualDistance;
	}

	DistanceUnit getDistanceUnit() {
		return distanceUnit;
	}

	Integer getActualRestSeconds() {
		return actualRestSeconds;
	}

	BigDecimal getActualRpe() {
		return actualRpe;
	}

	Instant getStartedAt() {
		return startedAt;
	}

	Instant getCompletedAt() {
		return completedAt;
	}

	String getAthleteNotes() {
		return athleteNotes;
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

	void setStatus(WorkoutExerciseExecutionStatus status) {
		this.status = status;
	}

	void setActualSets(Integer actualSets) {
		this.actualSets = actualSets;
	}

	void setActualReps(Integer actualReps) {
		this.actualReps = actualReps;
	}

	void setActualWeight(BigDecimal actualWeight) {
		this.actualWeight = actualWeight;
	}

	void setWeightUnit(WeightUnit weightUnit) {
		this.weightUnit = weightUnit;
	}

	void setActualDurationSeconds(Integer actualDurationSeconds) {
		this.actualDurationSeconds = actualDurationSeconds;
	}

	void setActualDistance(BigDecimal actualDistance) {
		this.actualDistance = actualDistance;
	}

	void setDistanceUnit(DistanceUnit distanceUnit) {
		this.distanceUnit = distanceUnit;
	}

	void setActualRestSeconds(Integer actualRestSeconds) {
		this.actualRestSeconds = actualRestSeconds;
	}

	void setActualRpe(BigDecimal actualRpe) {
		this.actualRpe = actualRpe;
	}

	void setStartedAt(Instant startedAt) {
		this.startedAt = startedAt;
	}

	void setCompletedAt(Instant completedAt) {
		this.completedAt = completedAt;
	}

	void setAthleteNotes(String athleteNotes) {
		this.athleteNotes = athleteNotes;
	}

	void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	void setPerformedExerciseDefinitionId(UUID performedExerciseDefinitionId) {
		this.performedExerciseDefinitionId = performedExerciseDefinitionId;
	}

	void setPerformedExerciseNameSnapshot(String performedExerciseNameSnapshot) {
		this.performedExerciseNameSnapshot = performedExerciseNameSnapshot;
	}

	void setPerformedExerciseCategorySnapshot(com.devinolabs.uap.training.domain.ExerciseDefinitionCategory performedExerciseCategorySnapshot) {
		this.performedExerciseCategorySnapshot = performedExerciseCategorySnapshot;
	}

	void setPerformedPrimaryMovementPatternSnapshot(com.devinolabs.uap.training.domain.MovementPattern performedPrimaryMovementPatternSnapshot) {
		this.performedPrimaryMovementPatternSnapshot = performedPrimaryMovementPatternSnapshot;
	}

	void setPerformedImpactLevelSnapshot(com.devinolabs.uap.training.domain.ImpactLevel performedImpactLevelSnapshot) {
		this.performedImpactLevelSnapshot = performedImpactLevelSnapshot;
	}

	void setExercisePerformanceKey(UUID exercisePerformanceKey) {
		this.exercisePerformanceKey = exercisePerformanceKey;
	}

	void setSubstitutionReason(com.devinolabs.uap.training.domain.ExerciseSubstitutionReason substitutionReason) {
		this.substitutionReason = substitutionReason;
	}

	void setSubstitutionNotes(String substitutionNotes) {
		this.substitutionNotes = substitutionNotes;
	}

	void setSubstitutedAt(Instant substitutedAt) {
		this.substitutedAt = substitutedAt;
	}

	com.devinolabs.uap.training.domain.ExerciseDefinitionCategory getPerformedExerciseCategorySnapshot() {
		return performedExerciseCategorySnapshot;
	}

	com.devinolabs.uap.training.domain.MovementPattern getPerformedPrimaryMovementPatternSnapshot() {
		return performedPrimaryMovementPatternSnapshot;
	}

	com.devinolabs.uap.training.domain.ImpactLevel getPerformedImpactLevelSnapshot() {
		return performedImpactLevelSnapshot;
	}

}
