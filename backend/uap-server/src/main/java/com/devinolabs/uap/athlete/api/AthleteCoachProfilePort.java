package com.devinolabs.uap.athlete.api;

import java.util.UUID;

/**
 * Read-only profile and active goals for the authenticated athlete.
 * Callers must not use this port to update the athlete.
 */
public interface AthleteCoachProfilePort {

	AthleteCoachProfileView profileForAccount(UUID accountId);

}
