package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class ProviderNotificationApplySupportTests {

	@Test
	void retriesOptimisticConflictsThenSucceeds() {
		TransactionTemplate transactions = mock(TransactionTemplate.class);
		AtomicInteger attempts = new AtomicInteger();
		doAnswer(invocation -> {
			@SuppressWarnings("unchecked")
			Consumer<TransactionStatus> action = invocation.getArgument(0);
			action.accept(mock(TransactionStatus.class));
			return null;
		}).when(transactions).executeWithoutResult(any());

		ProviderNotificationApplySupport.applyWithOptimisticRetry(transactions, () -> {
			if (attempts.incrementAndGet() < 2) {
				throw new ObjectOptimisticLockingFailureException("Subscription", "1");
			}
		});

		verify(transactions, times(2)).executeWithoutResult(any());
	}

	@Test
	void throwsLastConflictAfterExhaustedRetries() {
		TransactionTemplate transactions = mock(TransactionTemplate.class);
		doThrow(new ObjectOptimisticLockingFailureException("Subscription", "1"))
				.when(transactions)
				.executeWithoutResult(any());

		assertThatThrownBy(() -> ProviderNotificationApplySupport.applyWithOptimisticRetry(
						transactions, () -> {
						}))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		verify(transactions, times(ProviderNotificationApplySupport.MAX_APPLY_ATTEMPTS))
				.executeWithoutResult(any());
	}

}
