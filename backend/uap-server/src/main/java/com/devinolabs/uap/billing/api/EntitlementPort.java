package com.devinolabs.uap.billing.api;

import java.util.Set;
import java.util.UUID;

/**
 * Published provider-neutral entitlement lookup ({@code billing :: entitlements}).
 *
 * <p>Answers WHETHER the commercial subject currently possesses a purchased capability.
 * Does not answer authorization (WHO). Does not expose provider IDs or mutable aggregates.
 */
public interface EntitlementPort {

	boolean hasCapability(BillingSubjectType subjectType, UUID subjectId, CommercialCapability capability);

	Set<CommercialCapability> capabilities(BillingSubjectType subjectType, UUID subjectId);

}
