package com.devinolabs.uap.billing.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shared billing transaction template for Stripe, Apple, and Google Play webhook/notification apply paths.
 */
@Configuration
class BillingTransactionConfiguration {

	@Bean
	TransactionTemplate billingTransactions(PlatformTransactionManager transactionManager) {
		return new TransactionTemplate(transactionManager);
	}

}
