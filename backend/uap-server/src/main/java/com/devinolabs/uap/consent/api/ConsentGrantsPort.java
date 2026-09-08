package com.devinolabs.uap.consent.api;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Published consent grant capabilities for other modules.
 *
 * <p>Effective access is fail-closed and membership-generation bound (Slice C).
 */
public interface ConsentGrantsPort {

	boolean hasEffectiveScope(UUID athleteId, UUID teamId, String scope);

	Set<String> effectiveScopes(UUID athleteId, UUID teamId);

	/**
	 * Effective scopes for the supplied current athlete memberships on one Team.
	 * Keys are athlete IDs. Values are scope names. Generation-bound: a grant is
	 * included only when its membership id is in {@code currentMembershipIdToAthleteId}.
	 */
	Map<UUID, Set<String>> effectiveScopesForCurrentMemberships(
			UUID teamId,
			Map<UUID, UUID> currentMembershipIdToAthleteId);

}
