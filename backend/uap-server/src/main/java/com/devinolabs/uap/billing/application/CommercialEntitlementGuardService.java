package com.devinolabs.uap.billing.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.entitlements.CommercialCapability;
import com.devinolabs.uap.entitlements.CommercialEntitlementGuard;
import com.devinolabs.uap.entitlements.CommercialEntitlementRequiredException;
import com.devinolabs.uap.entitlements.EntitlementPort;

@Service
class CommercialEntitlementGuardService implements CommercialEntitlementGuard {

	private final EntitlementEnforcementProperties properties;
	private final EntitlementPort entitlementPort;

	CommercialEntitlementGuardService(EntitlementEnforcementProperties properties, EntitlementPort entitlementPort) {
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
		this.entitlementPort = Objects.requireNonNull(entitlementPort, "entitlementPort must not be null");
	}

	@Override
	public void requireOrganizationCapability(UUID organizationId, CommercialCapability capability) {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Objects.requireNonNull(capability, "capability must not be null");
		if (capability == CommercialCapability.INDIVIDUAL_PREMIUM) {
			throw new IllegalArgumentException("Organization product edges cannot require INDIVIDUAL_PREMIUM");
		}
		if (!properties.isEnabled()) {
			return;
		}
		if (!entitlementPort.hasCapability(BillingSubjectType.ORGANIZATION, organizationId, capability)) {
			throw new CommercialEntitlementRequiredException();
		}
	}

}
