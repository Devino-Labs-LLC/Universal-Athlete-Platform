package com.devinolabs.uap.billing.infrastructure.google;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConditionalOnProperty(prefix = "uap.billing.google-play", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GooglePlayBillingProperties.class)
class GooglePlayBillingConfiguration {

	GooglePlayBillingConfiguration(GooglePlayBillingProperties properties, Environment environment) {
		properties.validateWhenEnabled();
		for (String profile : environment.getActiveProfiles()) {
			if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
				throw new IllegalStateException("Google Play billing is sandbox-only in V4 G3");
			}
		}
	}

}
