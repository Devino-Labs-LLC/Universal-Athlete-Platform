package com.devinolabs.uap.integrations.infrastructure.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record DeviceWriteRequest(
		@NotNull UUID connectionId,
		@NotBlank @Size(max = 191) String externalDeviceId,
		@Size(max = 40) String deviceType,
		@Size(max = 80) String manufacturer,
		@Size(max = 80) String model,
		@Size(max = 120) String displayName,
		@Size(max = 64) String firmwareVersion,
		List<String> capabilities) {
}
