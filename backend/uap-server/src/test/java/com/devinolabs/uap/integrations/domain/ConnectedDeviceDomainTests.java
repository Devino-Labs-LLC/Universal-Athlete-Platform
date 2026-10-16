package com.devinolabs.uap.integrations.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConnectedDeviceDomainTests {

	private static final Instant NOW = Instant.parse("2026-10-12T18:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void identityIsNotTheConnectionIdAndStaleIsComputed() {
		UUID account = UUID.randomUUID();
		UUID athlete = UUID.randomUUID();
		ConnectionId connection = ConnectionId.generate();
		ConnectedDevice device = ConnectedDevice.register(
				DeviceId.generate(),
				athlete,
				account,
				connection,
				HealthProviderKey.APPLE_HEALTHKIT,
				"watch-1",
				DeviceType.SMARTWATCH,
				"Apple",
				"Watch",
				"Apple Watch",
				"1.0",
				Set.of(DeviceCapability.HEART_RATE, DeviceCapability.SLEEP),
				CLOCK);
		assertThat(device.id().value()).isNotEqualTo(connection.value());
		assertThat(device.effectiveLifecycle(NOW)).isEqualTo(DeviceLifecycle.ACTIVE);
		device.disconnect(CLOCK);
		assertThat(device.effectiveLifecycle(NOW)).isEqualTo(DeviceLifecycle.DISCONNECTED);
	}
}
