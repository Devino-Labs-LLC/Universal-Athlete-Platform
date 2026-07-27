package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecord;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceMetricCalculator;
import com.devinolabs.uap.training.domain.ExercisePerformanceMetrics;
import com.devinolabs.uap.training.domain.PerformanceMeasurement;
import com.devinolabs.uap.training.domain.UnitNormalizationService;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;

final class TrainingPerformanceSupport {

	static final int DEFAULT_PAGE_SIZE = 20;

	static final int MAX_PAGE_SIZE = 100;

	static final int MIN_RECENT_DAYS = 1;

	static final int MAX_RECENT_DAYS = 366;

	static final int DEFAULT_RECENT_DAYS = 30;

	static final int MIN_RECENT_LIMIT = 1;

	static final int MAX_RECENT_LIMIT = 100;

	static final int DEFAULT_RECENT_LIMIT = 20;

	private TrainingPerformanceSupport() {
	}

	static AthleteRef requireAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireAthlete(accountId);
	}

	static AthleteRef requireMutableAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireMutableAthleteForUpdate(accountId);
	}

	static int requirePage(Integer page) {
		int resolved = page == null ? 0 : page;
		if (resolved < 0) {
			throw new InvalidTrainingPerformanceRangeException("page must be >= 0");
		}
		return resolved;
	}

	static int requireSize(Integer size) {
		int resolved = size == null ? DEFAULT_PAGE_SIZE : size;
		if (resolved < 1 || resolved > MAX_PAGE_SIZE) {
			throw new InvalidTrainingPerformanceRangeException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		return resolved;
	}

	static int requireRecentDays(Integer days) {
		int resolved = days == null ? DEFAULT_RECENT_DAYS : days;
		if (resolved < MIN_RECENT_DAYS || resolved > MAX_RECENT_DAYS) {
			throw new InvalidTrainingPerformanceRangeException(
					"days must be between " + MIN_RECENT_DAYS + " and " + MAX_RECENT_DAYS);
		}
		return resolved;
	}

	static int requireRecentLimit(Integer limit) {
		int resolved = limit == null ? DEFAULT_RECENT_LIMIT : limit;
		if (resolved < MIN_RECENT_LIMIT || resolved > MAX_RECENT_LIMIT) {
			throw new InvalidTrainingPerformanceRangeException(
					"limit must be between " + MIN_RECENT_LIMIT + " and " + MAX_RECENT_LIMIT);
		}
		return resolved;
	}

	static void requireScheduledRange(java.time.LocalDate from, java.time.LocalDate to) {
		if (from != null && to != null && to.isBefore(from)) {
			throw new InvalidTrainingPerformanceRangeException("scheduledTo must not be before scheduledFrom");
		}
	}

	static Map<WorkoutExerciseExecutionId, List<WorkoutExerciseSet>> setsByExecution(
			WorkoutExerciseSetRepository setRepository,
			List<WorkoutExerciseExecutionId> executionIds,
			AthleteId athleteId) {
		Map<WorkoutExerciseExecutionId, List<WorkoutExerciseSet>> grouped = new HashMap<>();
		for (WorkoutExerciseSet set : setRepository.findAllByExecutionIdsAndAthleteId(executionIds, athleteId)) {
			grouped.computeIfAbsent(set.workoutExerciseExecutionId(), id -> new ArrayList<>()).add(set);
		}
		return grouped;
	}

	static ExerciseExecutionPerformanceResult toExecutionResult(
			ExercisePerformanceExecutionRow row,
			List<WorkoutExerciseSet> sets) {
		WorkoutExerciseExecution execution = row.execution();
		return new ExerciseExecutionPerformanceResult(
				execution.id(),
				execution.workoutOccurrenceId(),
				execution.exercisePerformanceKey(),
				execution.exerciseName(),
				execution.category(),
				execution.type(),
				execution.displayOrder(),
				execution.status(),
				row.scheduledDate(),
				execution.completedAt(),
				ExercisePerformanceMetricCalculator.calculate(sets));
	}

	static WorkoutOccurrencePerformanceResult.Totals totals(List<ExerciseExecutionPerformanceResult> exercises) {
		int completedExerciseCount = 0;
		int completedSetCount = 0;
		Integer totalRepetitions = null;
		Integer totalDurationSeconds = null;
		BigDecimal totalVolume = null;
		BigDecimal totalDistance = null;
		BigDecimal rpeTotal = BigDecimal.ZERO;
		int rpeCount = 0;
		for (ExerciseExecutionPerformanceResult exercise : exercises) {
			ExercisePerformanceMetrics metrics = exercise.metrics();
			if (metrics.completedSetCount() == 0) {
				continue;
			}
			completedExerciseCount++;
			completedSetCount += metrics.completedSetCount();
			totalRepetitions = sum(totalRepetitions, metrics.totalRepetitions());
			totalDurationSeconds = sum(totalDurationSeconds, metrics.totalDurationSeconds());
			totalVolume = add(totalVolume, metrics.totalVolume());
			totalDistance = add(totalDistance, metrics.totalDistance());
			if (metrics.averageRpe() != null) {
				rpeTotal = rpeTotal.add(metrics.averageRpe().multiply(
						BigDecimal.valueOf(metrics.completedSetCount())));
				rpeCount += metrics.completedSetCount();
			}
		}
		return new WorkoutOccurrencePerformanceResult.Totals(
				completedExerciseCount,
				completedSetCount,
				totalRepetitions,
				totalVolume,
				totalDurationSeconds,
				totalDistance,
				rpeCount == 0
						? null
						: UnitNormalizationService.toRpeScale(
								rpeTotal.divide(BigDecimal.valueOf(rpeCount), MathContext.DECIMAL128)));
	}

	static PersonalRecordResult toResult(AthleteExercisePersonalRecord record) {
		PerformanceMeasurement measurement = record.measurement();
		return new PersonalRecordResult(
				record.id(),
				record.exercisePerformanceKey(),
				record.recordType(),
				record.recordQualifier(),
				record.exerciseName(),
				measurement.normalizedValue(),
				measurement.normalizedUnit(),
				measurement.measuredValue(),
				measurement.measuredUnit(),
				measurement.estimated(),
				record.repetitions(),
				record.weightValue(),
				record.weightUnit(),
				record.achievedAt(),
				record.scheduledDate(),
				record.sourceSetId(),
				record.sourceExecutionId(),
				record.sourceOccurrenceId(),
				record.createdAt(),
				record.updatedAt());
	}

	static List<PersonalRecordResult> toResults(List<AthleteExercisePersonalRecord> records) {
		return records.stream().map(TrainingPerformanceSupport::toResult).toList();
	}

	private static Integer sum(Integer total, Integer value) {
		if (value == null) {
			return total;
		}
		return total == null ? value : total + value;
	}

	private static BigDecimal add(BigDecimal total, PerformanceMeasurement measurement) {
		if (measurement == null) {
			return total;
		}
		return total == null
				? measurement.normalizedValue()
				: total.add(measurement.normalizedValue());
	}

}
