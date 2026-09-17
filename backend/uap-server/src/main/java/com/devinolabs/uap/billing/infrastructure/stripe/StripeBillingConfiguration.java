package com.devinolabs.uap.billing.infrastructure.stripe;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.stripe.StripeClient;

@Configuration
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(StripeBillingProperties.class)
class StripeBillingConfiguration {

	@Bean
	StripeClient stripeClient(StripeBillingProperties properties, Environment environment) {
		properties.validateSandbox();
		for (String profile : environment.getActiveProfiles()) {
			if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
				throw new IllegalStateException("Stripe Organization billing is sandbox-only in V4 Slice B");
			}
		}
		return new StripeClient(properties.getSecretKey());
	}

	@Bean
	TransactionTemplate billingTransactions(PlatformTransactionManager transactionManager) {
		return new TransactionTemplate(transactionManager);
	}

}
