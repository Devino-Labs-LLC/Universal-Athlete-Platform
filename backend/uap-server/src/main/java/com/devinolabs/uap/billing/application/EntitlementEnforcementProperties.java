package com.devinolabs.uap.billing.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uap.billing.entitlement-enforcement")
public class EntitlementEnforcementProperties {

	/**
	 * When false (repository default), product edges do not return 402.
	 * Independent of {@code uap.billing.stripe.enabled}.
	 */
	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
