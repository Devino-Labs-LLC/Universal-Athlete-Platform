package com.devinolabs.uap.athlete.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Bounded profile for the AI Coach. Omits body measurements and date of birth.
 */
public record AthleteCoachProfileView(
		UUID athleteId,
		String firstName,
		String status,
		Instant profileUpdatedAt,
		List<String> activeGoalTitles) {
}
