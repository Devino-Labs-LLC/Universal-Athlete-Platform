package com.devinolabs.uap.integrations.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.SyncRun;
import com.devinolabs.uap.integrations.domain.SyncRunId;
import com.devinolabs.uap.integrations.domain.SyncRunStatus;

class DeferredLiveWearableAdapterTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T18:00:00Z"), ZoneOffset.UTC);

	@Test
	void liveGarminPullFailsHonestlyWithoutFabricatingSuccess() {
		DeferredLiveWearableAdapter adapter = new DeferredLiveWearableAdapter(HealthProviderKey.GARMIN, false);
		SyncRun run = SyncRun.request(SyncRunId.generate(), ConnectionId.generate(), UUID.randomUUID(), CLOCK);
		Connection connection = Connection.beginPending(
				ConnectionId.generate(), UUID.randomUUID(), UUID.randomUUID(), HealthProviderKey.GARMIN, CLOCK);
		adapter.applyPull(connection, run, CLOCK);
		assertThat(run.status()).isEqualTo(SyncRunStatus.FAILED);
		assertThat(run.errorCode()).isEqualTo("PROVIDER_LIVE_UNAVAILABLE");
	}

	@Test
	void mockFlagSucceedsEmptyWithoutCallingAVendor() {
		DeferredLiveWearableAdapter adapter = new DeferredLiveWearableAdapter(HealthProviderKey.WHOOP, true);
		SyncRun run = SyncRun.request(SyncRunId.generate(), ConnectionId.generate(), UUID.randomUUID(), CLOCK);
		Connection connection = Connection.beginPending(
				ConnectionId.generate(), UUID.randomUUID(), UUID.randomUUID(), HealthProviderKey.WHOOP, CLOCK);
		adapter.applyPull(connection, run, CLOCK);
		assertThat(run.status()).isEqualTo(SyncRunStatus.SUCCEEDED);
		assertThat(run.recordsAccepted()).isZero();
	}
}
