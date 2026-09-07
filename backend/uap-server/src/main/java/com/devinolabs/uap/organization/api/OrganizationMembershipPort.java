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

	/**
	 * ACTIVE athlete membership for the athlete profile on the team ({@code role=ATHLETE}, matching athleteId).
	 */
	Optional<TeamMembershipRef> findActiveAthleteMembershipByAthleteIdAndTeamId(UUID athleteId, UUID teamId);

	/**
	 * ACTIVE team membership for the account on the team, any role.
	 * Callers apply capability rules; this lookup is not authorization by itself.
	 */
	Optional<TeamMembershipRef> findActiveTeamMembership(UUID accountId, UUID teamId);

	/**
	 * Fail-closed team visibility: ACTIVE team membership OR ACTIVE org membership on the team's org,
	 * with Team ACTIVE and Organization ACTIVE.
	 */
	boolean canViewTeam(UUID accountId, UUID teamId);

	Optional<TeamLifecycleRef> findTeamLifecycle(UUID teamId);

}
