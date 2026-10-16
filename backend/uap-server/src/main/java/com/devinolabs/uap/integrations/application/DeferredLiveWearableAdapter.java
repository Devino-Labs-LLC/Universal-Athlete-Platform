package com.devinolabs.uap.integrations.application;

import java.time.Clock;
import java.util.Objects;

import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.SyncRun;

/**
 * Compiles and is testable. Live OAuth is not fabricated; pull fails honestly unless mock is on.
 */
public final class DeferredLiveWearableAdapter implements WearableProviderAdapter {

	private final HealthProviderKey provider;
	private final boolean mock;

	public DeferredLiveWearableAdapter(HealthProviderKey provider, boolean mock) {
		this.provider = Objects.requireNonNull(provider);
		this.mock = mock;
	}

	@Override
	public HealthProviderKey provider() {
		return provider;
	}

	@Override
	public void applyPull(Connection connection, SyncRun syncRun, Clock clock) {
		if (mock) {
			syncRun.markRunning(clock);
			syncRun.completeSucceededEmpty(clock);
			return;
		}
		syncRun.completeProviderLiveUnavailable(clock);
	}
}
