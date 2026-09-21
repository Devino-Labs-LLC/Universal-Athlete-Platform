package com.devinolabs.uap.entitlements;

import java.util.UUID;

/**
 * Published provider-neutral Organization active-athlete capacity lookup.
 *
 * <p>Does not expose Stripe types, billing JPA entities, or provider identifiers.
 */
public interface OrganizationCommercialCapacityPort {

	boolean isEnforcementEnabled();

	OrganizationCommercialCapacity resolve(UUID organizationId);

}
