package com.devinolabs.uap.billing.application;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Shared optimistic-retry apply loop for Apple and Google Play provider notification handlers.
 */
public final class ProviderNotificationApplySupport {

	static final int MAX_APPLY_ATTEMPTS = 3;

	private ProviderNotificationApplySupport() {
	}

	public static void applyWithOptimisticRetry(TransactionTemplate billingTransactions, Runnable apply) {
		ObjectOptimisticLockingFailureException lastConflict = null;
		for (int attempt = 1; attempt <= MAX_APPLY_ATTEMPTS; attempt++) {
			try {
				billingTransactions.executeWithoutResult(status -> apply.run());
				return;
			}
			catch (ObjectOptimisticLockingFailureException ex) {
				lastConflict = ex;
			}
		}
		throw lastConflict;
	}

}
