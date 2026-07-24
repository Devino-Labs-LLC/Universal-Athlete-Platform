package com.devinolabs.uap.athlete.api;

import java.util.UUID;

/**
 * Published Athlete context for other modules (Training, etc.).
 * Encapsulates athlete resolution, row locking, archived checks, and ownership.
 */
public interface AthleteContextPort {

	/**
	 * Resolves the athlete for the account under a PESSIMISTIC_WRITE lock.
	 * Rejects missing and archived athletes.
	 */
	AthleteRef requireMutableAthleteForUpdate(UUID accountId);

	/**
	 * Resolves the athlete for read operations without locking.
	 * Rejects missing athletes (archived athletes may still be read).
	 */
	AthleteRef requireAthlete(UUID accountId);

	/**
	 * No-op when {@code sportId} is null; otherwise requires the sport to belong to the athlete.
	 */
	void assertOptionalSportOwned(UUID athleteId, UUID sportId);

	/**
	 * No-op when {@code goalId} is null; otherwise requires the goal to belong to the athlete.
	 */
	void assertOptionalGoalOwned(UUID athleteId, UUID goalId);

}
