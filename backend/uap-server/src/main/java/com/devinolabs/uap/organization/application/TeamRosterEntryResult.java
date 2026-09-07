package com.devinolabs.uap.organization.application;

import java.util.UUID;

/**
 * Roster-safe athlete entry for coach/team roster reads.
 */
public record TeamRosterEntryResult(
		UUID athleteId,
		UUID membershipId,
		String displayName,
		String role,
		String status) {
}
