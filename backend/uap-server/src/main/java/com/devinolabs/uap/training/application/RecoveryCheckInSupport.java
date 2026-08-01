package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

final class RecoveryCheckInSupport {

	static final int MAX_LIST_RANGE_DAYS = 366;
	static final int MAX_CALENDAR_RANGE_DAYS = 93;
	static final int DEFAULT_PAGE_SIZE = 20;
	static final int MAX_PAGE_SIZE = 100;

	private RecoveryCheckInSupport() {
	}

	static AthleteRef requireMutableAthlete(AthleteContextPort athleteContextPort, java.util.UUID accountId) {
		return athleteContextPort.requireMutableAthleteForUpdate(accountId);
	}

	static AthleteRef requireReadableAthlete(AthleteContextPort athleteContextPort, java.util.UUID accountId) {
		return athleteContextPort.requireAthlete(accountId);
	}

	static void requireListDateRange(LocalDate startDate, LocalDate endDate) {
		requireOrderedRange(startDate, endDate, MAX_LIST_RANGE_DAYS, InvalidRecoveryCheckInDateRangeException::new);
	}

	static void requireCalendarDateRange(LocalDate startDate, LocalDate endDate) {
		requireOrderedRange(startDate, endDate, MAX_CALENDAR_RANGE_DAYS, InvalidRecoveryCalendarDateRangeException::new);
	}

	static int normalizePage(Integer page) {
		return page == null || page < 0 ? 0 : page;
	}

	static int normalizeSize(Integer size) {
		if (size == null || size <= 0) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(size, MAX_PAGE_SIZE);
	}

	static Map<LocalDate, DailyRecoveryCheckIn> indexByDate(List<DailyRecoveryCheckIn> checkIns) {
		return checkIns.stream().collect(Collectors.toMap(DailyRecoveryCheckIn::checkInDate, checkIn -> checkIn));
	}

	static Map<LocalDate, DailyTrainingLoadSummary> indexLoadByDate(List<DailyTrainingLoadSummary> summaries) {
		Map<LocalDate, DailyTrainingLoadSummary> indexed = new HashMap<>();
		for (DailyTrainingLoadSummary summary : summaries) {
			indexed.put(summary.date(), summary);
		}
		return indexed;
	}

	static Map<LocalDate, WorkoutDayCounts> countWorkoutsByDate(List<WorkoutOccurrence> occurrences) {
		Map<LocalDate, WorkoutDayCounts> counts = new HashMap<>();
		for (WorkoutOccurrence occurrence : occurrences) {
			LocalDate date = occurrence.scheduledDate();
			WorkoutDayCounts current = counts.getOrDefault(date, new WorkoutDayCounts(0, 0));
			if (isScheduledCount(occurrence.status())) {
				current = current.withScheduled(current.scheduled() + 1);
			}
			if (occurrence.status() == WorkoutOccurrenceStatus.COMPLETED) {
				current = current.withCompleted(current.completed() + 1);
			}
			counts.put(date, current);
		}
		return counts;
	}

	static RecoveryTrainingLoadContextResult toLoadContext(DailyTrainingLoadSummary summary) {
		if (summary == null) {
			return null;
		}
		return new RecoveryTrainingLoadContextResult(
				summary.date(),
				summary.occurrenceCount(),
				summary.ratedOccurrenceCount(),
				summary.unratedOccurrenceCount(),
				summary.completedExerciseCount(),
				summary.completedSetCount(),
				summary.totalVolumeKilograms(),
				summary.totalDurationSeconds(),
				summary.totalDistanceMeters(),
				summary.totalSessionRpeLoad());
	}

	static RecoveryTrainingLoadContextResult loadContextForDate(
			LocalDate date,
			Map<LocalDate, DailyTrainingLoadSummary> loadByDate) {
		DailyTrainingLoadSummary summary = loadByDate.get(date);
		if (summary == null) {
			return RecoveryTrainingLoadContextResult.empty(date);
		}
		return toLoadContext(summary);
	}

	private static boolean isScheduledCount(WorkoutOccurrenceStatus status) {
		return status == WorkoutOccurrenceStatus.SCHEDULED
				|| status == WorkoutOccurrenceStatus.IN_PROGRESS
				|| status == WorkoutOccurrenceStatus.COMPLETED;
	}

	private static void requireOrderedRange(
			LocalDate startDate,
			LocalDate endDate,
			int maxDays,
			java.util.function.Function<String, RuntimeException> exceptionFactory) {
		Objects.requireNonNull(startDate, "startDate must not be null");
		Objects.requireNonNull(endDate, "endDate must not be null");
		if (endDate.isBefore(startDate)) {
			throw exceptionFactory.apply("endDate must not be before startDate");
		}
		long span = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		if (span > maxDays) {
			throw exceptionFactory.apply("Date range must not exceed " + maxDays + " days");
		}
	}

	record WorkoutDayCounts(long scheduled, long completed) {

		WorkoutDayCounts withScheduled(long scheduled) {
			return new WorkoutDayCounts(scheduled, completed);
		}

		WorkoutDayCounts withCompleted(long completed) {
			return new WorkoutDayCounts(scheduled, completed);
		}
	}

}
