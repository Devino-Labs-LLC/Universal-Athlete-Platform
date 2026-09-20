package com.devinolabs.uap.entitlements;

import java.util.UUID;

/**
 * Published product-edge commercial enforcement.
 *
 * <p>No-op when {@code UAP_BILLING_ENTITLEMENT_ENFORCEMENT_ENABLED} is false.
 * Call only after V3 authorization has already succeeded for the resource graph.
 */
public interface CommercialEntitlementGuard {

	void requireOrganizationCapability(UUID organizationId, CommercialCapability capability);

}
