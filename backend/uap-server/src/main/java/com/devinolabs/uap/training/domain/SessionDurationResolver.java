package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;

/**
 * Resolves session duration minutes for effort and load summaries.
 */
public final class SessionDurationResolver {

	private static final int MIN_MINUTES = 1;
	private static final int MAX_MINUTES = 1440;

	private SessionDurationResolver() {
	}

	public record ResolvedSessionDuration(Integer minutes, SessionDurationSource source) {
	}

	public static ResolvedSessionDuration resolve(Integer athleteReportedMinutes, WorkoutOccurrence occurrence) {
		Objects.requireNonNull(occurrence, "occurrence must not be null");
		if (athleteReportedMinutes != null) {
			validateExplicitDuration(athleteReportedMinutes);
			return new ResolvedSessionDuration(athleteReportedMinutes, SessionDurationSource.ATHLETE_REPORTED);
		}
		if (occurrence.startedAt() != null && occurrence.completedAt() != null) {
			long seconds = Duration.between(occurrence.startedAt(), occurrence.completedAt()).getSeconds();
			int minutes = BigDecimal.valueOf(seconds)
					.divide(BigDecimal.valueOf(60), 0, RoundingMode.HALF_UP)
					.intValue();
			if (minutes < MIN_MINUTES) {
				minutes = MIN_MINUTES;
			}
			if (minutes > MAX_MINUTES) {
				minutes = MAX_MINUTES;
			}
			return new ResolvedSessionDuration(minutes, SessionDurationSource.OCCURRENCE_TIMESTAMPS);
		}
		return new ResolvedSessionDuration(null, SessionDurationSource.UNKNOWN);
	}

	public static void validateExplicitDuration(int sessionDurationMinutes) {
		if (sessionDurationMinutes < MIN_MINUTES || sessionDurationMinutes > MAX_MINUTES) {
			throw new InvalidSessionDurationException(
					"sessionDurationMinutes must be between 1 and 1440 inclusive");
		}
	}

}
