package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Validates athlete-local check-in dates.
 *
 * <p>Policy: today and up to 30 calendar days in the past (via injected {@link Clock}). Future dates
 * are rejected. Athlete-local date uses {@link LocalDate#now(Clock)} — same convention as
 * {@code scheduledDate} on workout occurrences.
 */
public final class RecoveryCheckInDateValidator {

	public static final int MAX_PAST_DAYS = 30;

	private RecoveryCheckInDateValidator() {
	}

	public static void validate(LocalDate checkInDate, Clock clock) {
		Objects.requireNonNull(checkInDate, "checkInDate must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		LocalDate today = LocalDate.now(clock);
		if (checkInDate.isAfter(today)) {
			throw new InvalidRecoveryCheckInDateException(
					"checkInDate must not be in the future");
		}
		if (checkInDate.isBefore(today.minusDays(MAX_PAST_DAYS))) {
			throw new RecoveryCheckInDateOutOfRangeException(
					"checkInDate must be within the last " + MAX_PAST_DAYS + " days");
		}
	}

}
