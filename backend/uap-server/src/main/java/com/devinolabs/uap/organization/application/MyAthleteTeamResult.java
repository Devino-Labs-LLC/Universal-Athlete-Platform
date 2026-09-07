package com.devinolabs.uap.organization.application;

import java.util.Objects;
import java.util.UUID;

/**
 * Athlete-facing ACTIVE team membership with display names for consent team picker.
 */
public record MyAthleteTeamResult(
		UUID membershipId,
		UUID teamId,
		String teamName,
		UUID organizationId,
		String organizationName,
		UUID athleteId) {

	public MyAthleteTeamResult {
		Objects.requireNonNull(membershipId, "membershipId must not be null");
		Objects.requireNonNull(teamId, "teamId must not be null");
		Objects.requireNonNull(teamName, "teamName must not be null");
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Objects.requireNonNull(organizationName, "organizationName must not be null");
		Objects.requireNonNull(athleteId, "athleteId must not be null");
	}

}
