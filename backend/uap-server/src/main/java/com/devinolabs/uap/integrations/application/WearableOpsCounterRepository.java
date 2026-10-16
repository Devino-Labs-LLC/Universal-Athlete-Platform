package com.devinolabs.uap.integrations.application;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

public interface WearableOpsCounterRepository {

	void increment(
			HealthProviderKey provider,
			int syncAttempts,
			int syncSuccesses,
			int syncFailures,
			int evidenceAccepted,
			int evidenceRejected,
			int duplicates,
			int adapterErrors,
			String errorCategory);
}
