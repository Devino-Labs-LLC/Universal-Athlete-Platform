package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

final class WorkoutExerciseSetSupport {

	static final int MAX_SETS_PER_EXECUTION = 100;

	private static final int RPE_SCALE = 2;

	private WorkoutExerciseSetSupport() {
	}

	/**
	 * Seeds the sets an execution starts life with: one per prescribed set, or a single set when the
	 * prescription carries no usable set count.
	 */
	static List<WorkoutExerciseSet> createInitialSets(WorkoutExerciseExecution execution, Clock clock) {
		Integer prescribedSets = execution.prescribedSets();
		int count = prescribedSets != null && prescribedSets > 0 ? prescribedSets : 1;
		if (count > MAX_SETS_PER_EXECUTION) {
			count = MAX_SETS_PER_EXECUTION;
		}
		List<WorkoutExerciseSet> sets = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			sets.add(WorkoutExerciseSet.fromExecutionPrescription(execution, i + 1, i, clock));
		}
		return sets;
	}

	/**
	 * Skips every still-active set of an execution so a skipped parent leaves no open child.
	 */
	static void skipActiveSets(
			WorkoutExerciseSetRepository repository,
			WorkoutExerciseExecutionId executionId,
			AthleteId athleteId,
			Clock clock) {
		skipAll(repository, repository.findAllByExecutionIdAndAthleteId(executionId, athleteId), clock);
	}

	static void skipActiveSetsForOccurrence(
			WorkoutExerciseSetRepository repository,
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId,
			Clock clock) {
		skipAll(repository, repository.findAllByOccurrenceIdAndAthleteId(occurrenceId, athleteId), clock);
	}

	private static void skipAll(
			WorkoutExerciseSetRepository repository,
			List<WorkoutExerciseSet> candidates,
			Clock clock) {
		List<WorkoutExerciseSet> toPersist = new ArrayList<>();
		for (WorkoutExerciseSet set : candidates) {
			if (!set.status().isActive()) {
				continue;
			}
			set.skip(clock);
			toPersist.add(set);
		}
		if (!toPersist.isEmpty()) {
			repository.saveAll(toPersist);
		}
	}

	static WorkoutExerciseSet requireOwnedSet(
			WorkoutExerciseSetRepository repository,
			WorkoutExerciseSetId setId,
			WorkoutExerciseExecutionId executionId,
			AthleteId athleteId) {
		return repository
				.findByIdAndExecutionIdAndAthleteId(setId, executionId, athleteId)
				.orElseThrow(WorkoutExerciseSetNotFoundException::new);
	}

	static int nextSetNumber(List<WorkoutExerciseSet> sets) {
		return sets.stream().mapToInt(WorkoutExerciseSet::setNumber).max().orElse(0) + 1;
	}

	static int nextDisplayOrder(List<WorkoutExerciseSet> sets) {
		return sets.stream().mapToInt(WorkoutExerciseSet::displayOrder).max().orElse(-1) + 1;
	}

	/**
	 * Rewrites set numbers and display orders to a dense 1..N / 0..N-1 sequence. The intermediate
	 * high-offset pass keeps the (execution, set_number) and (execution, display_order) unique
	 * indexes satisfied while the rows swap places.
	 */
	static void resequence(
			List<WorkoutExerciseSet> orderedSets,
			WorkoutExerciseSetRepository repository,
			Clock clock) {
		if (orderedSets.isEmpty()) {
			return;
		}
		int offset = orderedSets.size() + 1000;
		for (int i = 0; i < orderedSets.size(); i++) {
			orderedSets.get(i).changeSetNumber(offset + i, clock);
			orderedSets.get(i).changeDisplayOrder(offset + i, clock);
		}
		List<WorkoutExerciseSet> parked = repository.saveAll(orderedSets);
		for (int i = 0; i < parked.size(); i++) {
			parked.get(i).changeSetNumber(i + 1, clock);
			parked.get(i).changeDisplayOrder(i, clock);
		}
		repository.saveAll(parked);
	}

	static Map<WorkoutExerciseExecutionId, WorkoutExerciseSetCounts> countsByExecution(
			WorkoutExerciseSetRepository repository,
			Collection<WorkoutExerciseExecutionId> executionIds,
			AthleteId athleteId) {
		if (executionIds.isEmpty()) {
			return Map.of();
		}
		Map<WorkoutExerciseExecutionId, Map<WorkoutExerciseSetStatus, Long>> grouped = new HashMap<>();
		for (WorkoutExerciseSetStatusCount count : repository.countByStatusForExecutions(executionIds, athleteId)) {
			grouped
					.computeIfAbsent(count.executionId(), id -> new EnumMap<>(WorkoutExerciseSetStatus.class))
					.merge(count.status(), count.count(), Long::sum);
		}
		Map<WorkoutExerciseExecutionId, WorkoutExerciseSetCounts> counts = new HashMap<>();
		for (WorkoutExerciseExecutionId executionId : executionIds) {
			Map<WorkoutExerciseSetStatus, Long> byStatus = grouped.getOrDefault(executionId, Map.of());
			int notStarted = intValue(byStatus.get(WorkoutExerciseSetStatus.NOT_STARTED));
			int inProgress = intValue(byStatus.get(WorkoutExerciseSetStatus.IN_PROGRESS));
			int completed = intValue(byStatus.get(WorkoutExerciseSetStatus.COMPLETED));
			int skipped = intValue(byStatus.get(WorkoutExerciseSetStatus.SKIPPED));
			counts.put(executionId, new WorkoutExerciseSetCounts(
					notStarted + inProgress + completed + skipped, notStarted, inProgress, completed, skipped));
		}
		return counts;
	}

	static WorkoutExerciseSetCounts countsOf(List<WorkoutExerciseSet> sets) {
		int notStarted = 0;
		int inProgress = 0;
		int completed = 0;
		int skipped = 0;
		for (WorkoutExerciseSet set : sets) {
			switch (set.status()) {
				case NOT_STARTED -> notStarted++;
				case IN_PROGRESS -> inProgress++;
				case COMPLETED -> completed++;
				case SKIPPED -> skipped++;
			}
		}
		return new WorkoutExerciseSetCounts(sets.size(), notStarted, inProgress, completed, skipped);
	}

	/**
	 * Rolls the completed sets of an execution up into the execution summary aggregates.
	 */
	static DerivedExecutionActuals deriveActuals(List<WorkoutExerciseSet> sets) {
		List<WorkoutExerciseSet> completed = sets.stream()
				.filter(set -> set.status() == WorkoutExerciseSetStatus.COMPLETED)
				.toList();

		Integer actualReps = sumIntegers(completed.stream().map(WorkoutExerciseSet::actualReps).toList());
		Integer actualDurationSeconds = sumIntegers(
				completed.stream().map(WorkoutExerciseSet::actualDurationSeconds).toList());

		BigDecimal actualWeight = null;
		WeightUnit weightUnit = null;
		boolean consistentWeight = true;
		for (WorkoutExerciseSet set : completed) {
			if (set.actualWeight() == null) {
				continue;
			}
			if (actualWeight == null) {
				actualWeight = set.actualWeight();
				weightUnit = set.actualWeightUnit();
				continue;
			}
			if (actualWeight.compareTo(set.actualWeight()) != 0 || weightUnit != set.actualWeightUnit()) {
				consistentWeight = false;
				break;
			}
		}
		if (!consistentWeight) {
			actualWeight = null;
			weightUnit = null;
		}

		BigDecimal actualDistance = null;
		DistanceUnit distanceUnit = null;
		for (WorkoutExerciseSet set : completed) {
			if (set.actualDistance() == null) {
				continue;
			}
			if (distanceUnit == null) {
				distanceUnit = set.actualDistanceUnit();
				actualDistance = set.actualDistance();
				continue;
			}
			if (distanceUnit != set.actualDistanceUnit()) {
				throw new IllegalArgumentException(
						"Completed sets must not mix distance units within one exercise execution");
			}
			actualDistance = actualDistance.add(set.actualDistance());
		}

		Integer actualRestSeconds = null;
		List<Integer> rests = completed.stream()
				.map(WorkoutExerciseSet::actualRestSeconds)
				.filter(Objects::nonNull)
				.toList();
		if (!rests.isEmpty()) {
			BigDecimal total = BigDecimal.valueOf(rests.stream().mapToLong(Integer::longValue).sum());
			actualRestSeconds = total
					.divide(BigDecimal.valueOf(rests.size()), 0, RoundingMode.HALF_UP)
					.intValueExact();
		}

		BigDecimal actualRpe = null;
		List<BigDecimal> rpes = completed.stream()
				.map(WorkoutExerciseSet::actualRpe)
				.filter(Objects::nonNull)
				.toList();
		if (!rpes.isEmpty()) {
			BigDecimal total = rpes.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
			actualRpe = total.divide(BigDecimal.valueOf(rpes.size()), RPE_SCALE, RoundingMode.HALF_UP);
		}

		return new DerivedExecutionActuals(
				completed.size(),
				actualReps,
				actualWeight,
				weightUnit,
				actualDurationSeconds,
				actualDistance,
				distanceUnit,
				actualRestSeconds,
				actualRpe);
	}

	static void applyDerivedActuals(WorkoutExerciseExecution execution, DerivedExecutionActuals derived, Clock clock) {
		execution.applyDerivedActuals(
				derived.actualSets(),
				derived.actualReps(),
				derived.actualWeight(),
				derived.weightUnit(),
				derived.actualDurationSeconds(),
				derived.actualDistance(),
				derived.distanceUnit(),
				derived.actualRestSeconds(),
				derived.actualRpe(),
				clock);
	}

	static WorkoutExerciseSetResult toResult(WorkoutExerciseSet set) {
		return new WorkoutExerciseSetResult(
				set.id(),
				set.workoutExerciseExecutionId(),
				set.setNumber(),
				set.displayOrder(),
				set.setType(),
				set.prescribedMinimumReps(),
				set.prescribedMaximumReps(),
				set.prescribedWeight(),
				set.prescribedWeightUnit(),
				set.prescribedDurationSeconds(),
				set.prescribedDistance(),
				set.prescribedDistanceUnit(),
				set.prescribedTargetRpe(),
				set.prescribedRestSeconds(),
				set.actualReps(),
				set.actualWeight(),
				set.actualWeightUnit(),
				set.actualDurationSeconds(),
				set.actualDistance(),
				set.actualDistanceUnit(),
				set.actualRestSeconds(),
				set.actualRpe(),
				set.status(),
				set.startedAt(),
				set.completedAt(),
				set.athleteNotes(),
				set.createdAt(),
				set.updatedAt());
	}

	static List<WorkoutExerciseSetResult> toResults(List<WorkoutExerciseSet> sets) {
		return sets.stream().map(WorkoutExerciseSetSupport::toResult).toList();
	}

	static RuntimeException translateStatus(IllegalStateException ex) {
		return new InvalidWorkoutExerciseSetStatusException(
				Objects.requireNonNullElse(ex.getMessage(), "Invalid workout exercise set status transition"));
	}

	private static Integer sumIntegers(List<Integer> values) {
		Integer total = null;
		for (Integer value : values) {
			if (value == null) {
				continue;
			}
			total = total == null ? value : total + value;
		}
		return total;
	}

	private static int intValue(Long value) {
		return value == null ? 0 : Math.toIntExact(value);
	}

	record DerivedExecutionActuals(
			Integer actualSets,
			Integer actualReps,
			BigDecimal actualWeight,
			WeightUnit weightUnit,
			Integer actualDurationSeconds,
			BigDecimal actualDistance,
			DistanceUnit distanceUnit,
			Integer actualRestSeconds,
			BigDecimal actualRpe) {
	}

}
