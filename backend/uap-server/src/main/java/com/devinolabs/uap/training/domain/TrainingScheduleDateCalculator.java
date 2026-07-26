package com.devinolabs.uap.training.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Resolves calendar dates for workout day placements.
 *
 * <p>A plan week is a fixed 7-day window anchored on {@code scheduleStartDate}; it is not aligned to
 * ISO calendar weeks. Week {@code n} spans
 * {@code [scheduleStartDate + (n-1) weeks, scheduleStartDate + (n-1) weeks + 6 days]}, and a weekday
 * placement resolves to the single date inside that window carrying the requested
 * {@link DayOfWeek}.
 *
 * <p>Example: with {@code scheduleStartDate = 2026-08-05} (a Wednesday), week 1 spans
 * {@code 2026-08-05 .. 2026-08-11}, so a Monday placement in week 1 resolves to {@code 2026-08-10}.
 */
public final class TrainingScheduleDateCalculator {

	private TrainingScheduleDateCalculator() {
	}

	public static LocalDate weekWindowStart(LocalDate scheduleStartDate, int planWeekNumber) {
		Objects.requireNonNull(scheduleStartDate, "scheduleStartDate must not be null");
		requirePlanWeekNumber(planWeekNumber);
		return scheduleStartDate.plusWeeks(planWeekNumber - 1L);
	}

	public static LocalDate weekWindowEnd(LocalDate scheduleStartDate, int planWeekNumber) {
		return weekWindowStart(scheduleStartDate, planWeekNumber).plusDays(6);
	}

	public static LocalDate placementDate(
			LocalDate scheduleStartDate,
			int planWeekNumber,
			DayOfWeek dayOfWeek) {
		Objects.requireNonNull(dayOfWeek, "dayOfWeek must not be null");
		LocalDate windowStart = weekWindowStart(scheduleStartDate, planWeekNumber);
		int offset = Math.floorMod(dayOfWeek.getValue() - windowStart.getDayOfWeek().getValue(), 7);
		return windowStart.plusDays(offset);
	}

	private static void requirePlanWeekNumber(int planWeekNumber) {
		if (planWeekNumber < 1) {
			throw new IllegalArgumentException("planWeekNumber must be at least 1");
		}
	}

}
