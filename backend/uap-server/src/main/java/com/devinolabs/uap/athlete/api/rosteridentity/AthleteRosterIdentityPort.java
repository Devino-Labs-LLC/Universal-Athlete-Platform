package com.devinolabs.uap.athlete.api.rosteridentity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Roster-safe athlete display identity for org/team surfaces.
 *
 * <p>Archived athletes are omitted. Missing ids return empty / absent map entries.
 */
public interface AthleteRosterIdentityPort {

	Optional<AthleteRosterIdentity> findByAthleteId(UUID athleteId);

	Map<UUID, AthleteRosterIdentity> findByAthleteIds(Collection<UUID> athleteIds);

	record AthleteRosterIdentity(UUID athleteId, String displayName) {
	}

}
