package com.devinolabs.uap.billing.infrastructure.apple;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnProperty(prefix = "uap.billing.apple", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(AppleBillingProperties.class)
class AppleBillingConfiguration {

	AppleBillingConfiguration(AppleBillingProperties properties, Environment environment) {
		properties.validateWhenEnabled();
		for (String profile : environment.getActiveProfiles()) {
			if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
				throw new IllegalStateException("Apple App Store billing is sandbox-only in V4 G2");
			}
		}
	}

}
