package com.devinolabs.uap.integrations.api.devices;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeviceView(
		UUID deviceId,
		UUID connectionId,
		String provider,
		String externalDeviceId,
		String deviceType,
		String manufacturer,
		String model,
		String displayName,
		String firmwareVersion,
		List<String> capabilities,
		String lifecycleState,
		Instant firstSeenAt,
		Instant lastSeenAt,
		Instant lastSuccessfulDataAt) {
}
