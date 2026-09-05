package com.devinolabs.uap.organization.api;

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

}
