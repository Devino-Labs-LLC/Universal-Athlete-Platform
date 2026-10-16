package com.devinolabs.uap.integrations.application;

import java.time.Clock;

import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.SyncRun;

/**
 * Provider-neutral pull adapter. Implementations must not write State Engine or Coach rows.
 */
public interface WearableProviderAdapter {

	HealthProviderKey provider();

	void applyPull(Connection connection, SyncRun syncRun, Clock clock);
}
