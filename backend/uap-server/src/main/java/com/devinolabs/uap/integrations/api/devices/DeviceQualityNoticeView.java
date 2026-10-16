package com.devinolabs.uap.integrations.api.devices;

import java.util.UUID;

public record DeviceQualityNoticeView(
		UUID deviceId,
		String code,
		String message) {
}
