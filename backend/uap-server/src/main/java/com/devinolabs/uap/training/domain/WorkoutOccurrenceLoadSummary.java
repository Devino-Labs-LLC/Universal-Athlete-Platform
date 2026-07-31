package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class WorkoutOccurrenceLoadSummary {

	private final WorkoutOccurrenceLoadSummaryId id;
	private final AthleteId athleteId;
	private final TrainingPlanId trainingPlanId;
	private final WorkoutDayId workoutDayId;
	private final WorkoutOccurrenceId workoutOccurrenceId;
	private final LocalDate scheduledDate;
	private final SessionRpe sessionRpe;
	private final Integer sessionDurationMinutes;
	private final SessionRpeLoad sessionRpeLoad;
	private final long prescribedExerciseCount;
	private final long completedExerciseCount;
	private final long substitutedExerciseCount;
	private final long completedSetCount;
	private final long skippedSetCount;
	private final long completedRepetitionCount;
	private final BigDecimal totalVolumeKilograms;
	private final long totalDurationSeconds;
	private final BigDecimal totalDistanceMeters;
	private final long noImpactExerciseCount;
	private final long lowImpactExerciseCount;
	private final long moderateImpactExerciseCount;
	private final long highImpactExerciseCount;
	private final List<WorkoutLoadCategorySummary> categorySummaries;
	private final List<WorkoutLoadMovementPatternSummary> movementSummaries;
	private final Instant calculatedAt;
	private final Instant sourceUpdatedAt;
	private final TrainingLoadCalculationVersion calculationVersion;
	private final Instant createdAt;
	private final Instant updatedAt;
	private final long version;

	private WorkoutOccurrenceLoadSummary(
			WorkoutOccurrenceLoadSummaryId id,
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrenceId workoutOccurrenceId,
			LocalDate scheduledDate,
			SessionRpe sessionRpe,
			Integer sessionDurationMinutes,
			SessionRpeLoad sessionRpeLoad,
			long prescribedExerciseCount,
			long completedExerciseCount,
			long substitutedExerciseCount,
			long completedSetCount,
			long skippedSetCount,
			long completedRepetitionCount,
			BigDecimal totalVolumeKilograms,
			long totalDurationSeconds,
			BigDecimal totalDistanceMeters,
			long noImpactExerciseCount,
			long lowImpactExerciseCount,
			long moderateImpactExerciseCount,
			long highImpactExerciseCount,
			List<WorkoutLoadCategorySummary> categorySummaries,
			List<WorkoutLoadMovementPatternSummary> movementSummaries,
			Instant calculatedAt,
			Instant sourceUpdatedAt,
			TrainingLoadCalculationVersion calculationVersion,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.trainingPlanId = Objects.requireNonNull(trainingPlanId, "trainingPlanId must not be null");
		this.workoutDayId = Objects.requireNonNull(workoutDayId, "workoutDayId must not be null");
		this.workoutOccurrenceId = Objects.requireNonNull(workoutOccurrenceId, "workoutOccurrenceId must not be null");
		this.scheduledDate = Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");
		this.sessionRpe = sessionRpe;
		this.sessionDurationMinutes = sessionDurationMinutes;
		this.sessionRpeLoad = sessionRpeLoad;
		this.prescribedExerciseCount = prescribedExerciseCount;
		this.completedExerciseCount = completedExerciseCount;
		this.substitutedExerciseCount = substitutedExerciseCount;
		this.completedSetCount = completedSetCount;
		this.skippedSetCount = skippedSetCount;
		this.completedRepetitionCount = completedRepetitionCount;
		this.totalVolumeKilograms = Objects.requireNonNull(totalVolumeKilograms, "totalVolumeKilograms must not be null");
		this.totalDurationSeconds = totalDurationSeconds;
		this.totalDistanceMeters = Objects.requireNonNull(totalDistanceMeters, "totalDistanceMeters must not be null");
		this.noImpactExerciseCount = noImpactExerciseCount;
		this.lowImpactExerciseCount = lowImpactExerciseCount;
		this.moderateImpactExerciseCount = moderateImpactExerciseCount;
		this.highImpactExerciseCount = highImpactExerciseCount;
		this.categorySummaries = List.copyOf(categorySummaries);
		this.movementSummaries = List.copyOf(movementSummaries);
		this.calculatedAt = Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
		this.sourceUpdatedAt = Objects.requireNonNull(sourceUpdatedAt, "sourceUpdatedAt must not be null");
		this.calculationVersion = Objects.requireNonNull(calculationVersion, "calculationVersion must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	static WorkoutOccurrenceLoadSummary create(
			WorkoutOccurrenceLoadSummaryId id,
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrenceId workoutOccurrenceId,
			LocalDate scheduledDate,
			WorkoutLoadCalculator.Result result,
			Instant calculatedAt,
			Instant sourceUpdatedAt,
			Instant now,
			Clock clock) {
		return fromResult(
				id,
				athleteId,
				trainingPlanId,
				workoutDayId,
				workoutOccurrenceId,
				scheduledDate,
				result,
				calculatedAt,
				sourceUpdatedAt,
				now,
				now,
				0L);
	}

	public static WorkoutOccurrenceLoadSummary rehydrate(
			WorkoutOccurrenceLoadSummaryId id,
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrenceId workoutOccurrenceId,
			LocalDate scheduledDate,
			WorkoutLoadCalculator.Result result,
			Instant calculatedAt,
			Instant sourceUpdatedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return fromResult(
				id,
				athleteId,
				trainingPlanId,
				workoutDayId,
				workoutOccurrenceId,
				scheduledDate,
				result,
				calculatedAt,
				sourceUpdatedAt,
				createdAt,
				updatedAt,
				version);
	}

	private static WorkoutOccurrenceLoadSummary fromResult(
			WorkoutOccurrenceLoadSummaryId id,
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrenceId workoutOccurrenceId,
			LocalDate scheduledDate,
			WorkoutLoadCalculator.Result result,
			Instant calculatedAt,
			Instant sourceUpdatedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new WorkoutOccurrenceLoadSummary(
				id,
				athleteId,
				trainingPlanId,
				workoutDayId,
				workoutOccurrenceId,
				scheduledDate,
				result.sessionRpe(),
				result.sessionDurationMinutes(),
				result.sessionRpeLoad(),
				result.prescribedExerciseCount(),
				result.completedExerciseCount(),
				result.substitutedExerciseCount(),
				result.completedSetCount(),
				result.skippedSetCount(),
				result.completedRepetitionCount(),
				result.totalVolumeKilograms(),
				result.totalDurationSeconds(),
				result.totalDistanceMeters(),
				result.noImpactExerciseCount(),
				result.lowImpactExerciseCount(),
				result.moderateImpactExerciseCount(),
				result.highImpactExerciseCount(),
				result.categorySummaries(),
				result.movementSummaries(),
				calculatedAt,
				sourceUpdatedAt,
				result.calculationVersion(),
				createdAt,
				updatedAt,
				version);
	}

	public WorkoutOccurrenceLoadSummaryId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public TrainingPlanId trainingPlanId() {
		return trainingPlanId;
	}

	public WorkoutDayId workoutDayId() {
		return workoutDayId;
	}

	public WorkoutOccurrenceId workoutOccurrenceId() {
		return workoutOccurrenceId;
	}

	public LocalDate scheduledDate() {
		return scheduledDate;
	}

	public SessionRpe sessionRpe() {
		return sessionRpe;
	}

	public Integer sessionDurationMinutes() {
		return sessionDurationMinutes;
	}

	public SessionRpeLoad sessionRpeLoad() {
		return sessionRpeLoad;
	}

	public long prescribedExerciseCount() {
		return prescribedExerciseCount;
	}

	public long completedExerciseCount() {
		return completedExerciseCount;
	}

	public long substitutedExerciseCount() {
		return substitutedExerciseCount;
	}

	public long completedSetCount() {
		return completedSetCount;
	}

	public long skippedSetCount() {
		return skippedSetCount;
	}

	public long completedRepetitionCount() {
		return completedRepetitionCount;
	}

	public BigDecimal totalVolumeKilograms() {
		return totalVolumeKilograms;
	}

	public long totalDurationSeconds() {
		return totalDurationSeconds;
	}

	public BigDecimal totalDistanceMeters() {
		return totalDistanceMeters;
	}

	public long noImpactExerciseCount() {
		return noImpactExerciseCount;
	}

	public long lowImpactExerciseCount() {
		return lowImpactExerciseCount;
	}

	public long moderateImpactExerciseCount() {
		return moderateImpactExerciseCount;
	}

	public long highImpactExerciseCount() {
		return highImpactExerciseCount;
	}

	public List<WorkoutLoadCategorySummary> categorySummaries() {
		return categorySummaries;
	}

	public List<WorkoutLoadMovementPatternSummary> movementSummaries() {
		return movementSummaries;
	}

	public Instant calculatedAt() {
		return calculatedAt;
	}

	public Instant sourceUpdatedAt() {
		return sourceUpdatedAt;
	}

	public TrainingLoadCalculationVersion calculationVersion() {
		return calculationVersion;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}

}
