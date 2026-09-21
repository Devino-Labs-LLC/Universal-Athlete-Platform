package com.devinolabs.uap.billing.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uap.billing.organization-capacity-enforcement")
public class OrganizationCapacityEnforcementProperties {

	/**
	 * When false (repository default), invitation accept is not denied for band capacity.
	 * Independent of {@code uap.billing.stripe.enabled} and entitlement enforcement.
	 */
	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
