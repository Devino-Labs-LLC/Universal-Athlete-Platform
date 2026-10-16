package com.devinolabs.uap.integrations.domain;

import java.util.Locale;
import java.util.Optional;

public enum DeviceCapability {
	HEART_RATE,
	HRV,
	SLEEP,
	ACTIVITY,
	GPS,
	POWER,
	CADENCE,
	TEMPERATURE,
	RESPIRATORY_RATE,
	WORKOUTS;

	public boolean supports(SignalFamily family) {
		return switch (this) {
			case HEART_RATE -> family == SignalFamily.HEART;
			case HRV -> family == SignalFamily.HRV;
			case SLEEP -> family == SignalFamily.SLEEP;
			case ACTIVITY -> family == SignalFamily.ACTIVITY;
			case GPS -> family == SignalFamily.ACTIVITY || family == SignalFamily.WORKOUT;
			case POWER -> family == SignalFamily.POWER;
			case CADENCE -> family == SignalFamily.CADENCE;
			case TEMPERATURE -> family == SignalFamily.TEMPERATURE;
			case RESPIRATORY_RATE -> family == SignalFamily.RESPIRATORY;
			case WORKOUTS -> family == SignalFamily.WORKOUT;
		};
	}

	public static Optional<DeviceCapability> tryParse(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(DeviceCapability.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
}
