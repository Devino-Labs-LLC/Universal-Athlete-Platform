package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure occurrence load calculation from executions, sets, optional session effort, and occurrence
 * context.
 */
public final class WorkoutLoadCalculator {

	private static final BigDecimal ZERO_VOLUME = BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY);
	private static final BigDecimal ZERO_DISTANCE = BigDecimal.ZERO.setScale(3, RoundingMode.UNNECESSARY);

	private WorkoutLoadCalculator() {
	}

	public record Input(
			AthleteId athleteId,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			WorkoutOccurrenceId workoutOccurrenceId,
			LocalDate scheduledDate,
			List<WorkoutExerciseExecution> executions,
			Map<WorkoutExerciseExecutionId, List<WorkoutExerciseSet>> setsByExecution,
			WorkoutSessionEffort sessionEffort,
			WorkoutOccurrence occurrence,
			Instant sourceUpdatedAt) {
	}

	public record Result(
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
			TrainingLoadCalculationVersion calculationVersion) {
	}

	public static Result calculate(Input input) {
		Objects.requireNonNull(input, "input must not be null");
		List<WorkoutExerciseExecution> executions = input.executions();
		Map<WorkoutExerciseExecutionId, List<WorkoutExerciseSet>> setsByExecution =
				input.setsByExecution() == null ? Map.of() : input.setsByExecution();

		long prescribedExerciseCount = executions.size();
		long completedExerciseCount = 0;
		long substitutedExerciseCount = 0;
		long completedSetCount = 0;
		long skippedSetCount = 0;
		long completedRepetitionCount = 0;
		BigDecimal totalVolume = ZERO_VOLUME;
		long totalDurationSeconds = 0;
		BigDecimal totalDistance = ZERO_DISTANCE;

		Map<ImpactLevel, Long> impactCounts = new EnumMap<>(ImpactLevel.class);
		for (ImpactLevel level : ImpactLevel.values()) {
			impactCounts.put(level, 0L);
		}

		Map<ExerciseDefinitionCategory, CategoryAccumulator> categories = new HashMap<>();
		Map<MovementPattern, MovementAccumulator> movements = new HashMap<>();

		for (WorkoutExerciseExecution execution : executions) {
			List<WorkoutExerciseSet> sets = setsByExecution.getOrDefault(execution.id(), List.of());
			for (WorkoutExerciseSet set : sets) {
				if (set.status() == WorkoutExerciseSetStatus.COMPLETED) {
					completedSetCount++;
				}
				else if (set.status() == WorkoutExerciseSetStatus.SKIPPED) {
					skippedSetCount++;
				}
			}

			if (execution.status() != WorkoutExerciseExecutionStatus.COMPLETED) {
				continue;
			}
			completedExerciseCount++;
			if (execution.isSubstituted()) {
				substitutedExerciseCount++;
			}

			impactCounts.merge(execution.performedImpactLevelSnapshot(), 1L, Long::sum);

			List<WorkoutExerciseSet> completedSets = ExercisePerformanceMetricCalculator.eligibleSets(sets);
			long executionSetCount = completedSets.size();
			long executionReps = 0;
			BigDecimal executionVolume = ZERO_VOLUME;
			long executionDuration = 0;
			BigDecimal executionDistance = ZERO_DISTANCE;

			for (WorkoutExerciseSet set : completedSets) {
				Integer reps = set.actualReps();
				if (reps != null) {
					executionReps += reps;
					completedRepetitionCount += reps;
				}
				SetVolume volume = UnitNormalizationService.volumeOf(
						set.actualWeight(), set.actualWeightUnit(), reps);
				if (volume != null) {
					executionVolume = executionVolume.add(volume.kilogramRepetitions());
					totalVolume = totalVolume.add(volume.kilogramRepetitions());
				}
				Integer duration = set.actualDurationSeconds();
				if (duration != null && duration > 0) {
					executionDuration += duration;
					totalDurationSeconds += duration;
				}
				if (set.actualDistance() != null && set.actualDistanceUnit() != null) {
					NormalizedDistance distance = UnitNormalizationService.normalizeDistance(
							set.actualDistance(), set.actualDistanceUnit());
					if (distance.isPositive()) {
						executionDistance = executionDistance.add(distance.meters());
						totalDistance = totalDistance.add(distance.meters());
					}
				}
			}

			ExerciseDefinitionCategory category = execution.performedExerciseCategorySnapshot();
			CategoryAccumulator categoryAccumulator = categories.computeIfAbsent(category, CategoryAccumulator::new);
			categoryAccumulator.exerciseCount++;
			categoryAccumulator.setCount += executionSetCount;
			categoryAccumulator.volume = categoryAccumulator.volume.add(executionVolume);
			categoryAccumulator.durationSeconds += executionDuration;
			categoryAccumulator.distance = categoryAccumulator.distance.add(executionDistance);

			MovementPattern pattern = execution.performedPrimaryMovementPatternSnapshot();
			MovementAccumulator movementAccumulator = movements.computeIfAbsent(pattern, MovementAccumulator::new);
			movementAccumulator.exerciseCount++;
			movementAccumulator.setCount += executionSetCount;
			movementAccumulator.repetitionCount += executionReps;
			movementAccumulator.volume = movementAccumulator.volume.add(executionVolume);
			movementAccumulator.durationSeconds += executionDuration;
			movementAccumulator.distance = movementAccumulator.distance.add(executionDistance);
		}

		SessionRpe sessionRpe = input.sessionEffort() == null ? null : input.sessionEffort().sessionRpe();
		Integer sessionDurationMinutes = resolveSessionDurationMinutes(input);
		SessionRpeLoad sessionRpeLoad = SessionRpeLoad.ofNullable(sessionRpe, sessionDurationMinutes);

		List<WorkoutLoadCategorySummary> categorySummaries = categories.values().stream()
				.sorted(Comparator.comparing(a -> a.category.name()))
				.map(CategoryAccumulator::toSummary)
				.toList();
		List<WorkoutLoadMovementPatternSummary> movementSummaries = movements.values().stream()
				.sorted(Comparator.comparing(a -> a.pattern.name()))
				.map(MovementAccumulator::toSummary)
				.toList();

		return new Result(
				sessionRpe,
				sessionDurationMinutes,
				sessionRpeLoad,
				prescribedExerciseCount,
				completedExerciseCount,
				substitutedExerciseCount,
				completedSetCount,
				skippedSetCount,
				completedRepetitionCount,
				scaleVolume(totalVolume),
				totalDurationSeconds,
				scaleDistance(totalDistance),
				impactCounts.getOrDefault(ImpactLevel.NO_IMPACT, 0L),
				impactCounts.getOrDefault(ImpactLevel.LOW_IMPACT, 0L),
				impactCounts.getOrDefault(ImpactLevel.MODERATE_IMPACT, 0L),
				impactCounts.getOrDefault(ImpactLevel.HIGH_IMPACT, 0L),
				categorySummaries,
				movementSummaries,
				TrainingLoadCalculationVersion.current());
	}

	public static WorkoutOccurrenceLoadSummary toSummary(
			WorkoutOccurrenceLoadSummaryId id,
			Input input,
			Result result,
			Instant calculatedAt,
			Clock clock,
			long version,
			boolean isNew) {
		Objects.requireNonNull(input, "input must not be null");
		Objects.requireNonNull(result, "result must not be null");
		Instant now = Instant.now(clock);
		Instant sourceUpdatedAt = input.sourceUpdatedAt() == null ? now : input.sourceUpdatedAt();
		if (isNew) {
			return WorkoutOccurrenceLoadSummary.create(
					id,
					input.athleteId(),
					input.trainingPlanId(),
					input.workoutDayId(),
					input.workoutOccurrenceId(),
					input.scheduledDate(),
					result,
					calculatedAt,
					sourceUpdatedAt,
					now,
					clock);
		}
		return WorkoutOccurrenceLoadSummary.rehydrate(
				id,
				input.athleteId(),
				input.trainingPlanId(),
				input.workoutDayId(),
				input.workoutOccurrenceId(),
				input.scheduledDate(),
				result,
				calculatedAt,
				sourceUpdatedAt,
				now,
				now,
				version);
	}

	private static Integer resolveSessionDurationMinutes(Input input) {
		if (input.sessionEffort() != null && input.sessionEffort().sessionDurationMinutes() != null) {
			return input.sessionEffort().sessionDurationMinutes();
		}
		if (input.sessionEffort() != null
				&& input.sessionEffort().durationSource() == SessionDurationSource.UNKNOWN) {
			return null;
		}
		if (input.sessionEffort() != null) {
			return input.sessionEffort().sessionDurationMinutes();
		}
		SessionDurationResolver.ResolvedSessionDuration derived =
				SessionDurationResolver.resolve(null, input.occurrence());
		return derived.minutes();
	}

	private static BigDecimal scaleVolume(BigDecimal value) {
		try {
			return value.setScale(3, RoundingMode.HALF_UP);
		}
		catch (ArithmeticException ex) {
			throw new TrainingLoadNumericOverflowException("Volume total overflowed");
		}
	}

	private static BigDecimal scaleDistance(BigDecimal value) {
		try {
			return value.setScale(3, RoundingMode.HALF_UP);
		}
		catch (ArithmeticException ex) {
			throw new TrainingLoadNumericOverflowException("Distance total overflowed");
		}
	}

	private static final class CategoryAccumulator {

		private final ExerciseDefinitionCategory category;
		private long exerciseCount;
		private long setCount;
		private BigDecimal volume = ZERO_VOLUME;
		private long durationSeconds;
		private BigDecimal distance = ZERO_DISTANCE;

		private CategoryAccumulator(ExerciseDefinitionCategory category) {
			this.category = category;
		}

		private WorkoutLoadCategorySummary toSummary() {
			return new WorkoutLoadCategorySummary(
					category, exerciseCount, setCount, scaleVolume(volume), durationSeconds, scaleDistance(distance));
		}
	}

	private static final class MovementAccumulator {

		private final MovementPattern pattern;
		private long exerciseCount;
		private long setCount;
		private long repetitionCount;
		private BigDecimal volume = ZERO_VOLUME;
		private long durationSeconds;
		private BigDecimal distance = ZERO_DISTANCE;

		private MovementAccumulator(MovementPattern pattern) {
			this.pattern = pattern;
		}

		private WorkoutLoadMovementPatternSummary toSummary() {
			return new WorkoutLoadMovementPatternSummary(
					pattern,
					exerciseCount,
					setCount,
					repetitionCount,
					scaleVolume(volume),
					durationSeconds,
					scaleDistance(distance));
		}
	}

}
