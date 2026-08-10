package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;

/**
 * Shared validation and athlete resolution for client facade use cases.
 */
final class TrainingClientFacadeSupport {

	private static final Set<Integer> ALLOWED_TREND_DAYS = Set.of(7, 14, 28);

	private TrainingClientFacadeSupport() {
	}

	static LocalDate resolveDate(LocalDate date, Clock clock) {
		Objects.requireNonNull(clock, "clock must not be null");
		LocalDate today = LocalDate.now(clock);
		LocalDate resolved = date == null ? today : date;
		if (resolved.isAfter(today)) {
			throw new InvalidTrainingClientDateException("date must not be in the future");
		}
		long ageDays = ChronoUnit.DAYS.between(resolved, today);
		if (ageDays > DailyAthleteStateSupport.MAX_HISTORY_DAYS) {
			throw new InvalidTrainingClientDateException(
					"date must be within the last " + DailyAthleteStateSupport.MAX_HISTORY_DAYS + " days");
		}
		return resolved;
	}

	static int requireTrendDays(Integer trendDays) {
		int resolved = trendDays == null ? 7 : trendDays;
		if (!ALLOWED_TREND_DAYS.contains(resolved)) {
			throw new InvalidTrainingClientTrendDaysException(
					"trendDays must be one of 7, 14, or 28");
		}
		return resolved;
	}

	static AthleteRef requireReadableAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return DailyAthleteStateSupport.requireReadableAthlete(athleteContextPort, accountId);
	}

}
