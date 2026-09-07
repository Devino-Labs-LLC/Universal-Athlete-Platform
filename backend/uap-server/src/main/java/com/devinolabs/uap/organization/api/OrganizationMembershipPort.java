package com.devinolabs.uap.organization.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Published organization membership capabilities for other modules.
 */
public interface OrganizationMembershipPort {

	boolean hasActiveOrganizationMembership(UUID accountId, UUID organizationId);

	/**
	 * Slice A: ACTIVE membership with {@code ORG_OWNER} may manage the organization and its teams.
	 */
	boolean canManageOrganization(UUID accountId, UUID organizationId);

	/**
	 * Read-only membership snapshot for consent binding and effective-access checks.
	 */
	record TeamMembershipRef(
			UUID membershipId,
			UUID teamId,
			UUID organizationId,
			UUID accountId,
			UUID athleteId,
			String role,
			String status) {
	}

	/**
	 * Team + parent organization lifecycle statuses for fail-closed consent checks.
	 */
	record TeamLifecycleRef(
			UUID teamId,
			UUID organizationId,
			String teamName,
			String organizationName,
			String teamStatus,
			String organizationStatus) {
	}

	Optional<TeamMembershipRef> findTeamMembership(UUID membershipId);

	/**
	 * ACTIVE athlete membership for the account on the team ({@code role=ATHLETE}, non-null athleteId).
	 */
	Optional<TeamMembershipRef> findActiveAthleteTeamMembership(UUID accountId, UUID teamId);

	Optional<TeamLifecycleRef> findTeamLifecycle(UUID teamId);

}
