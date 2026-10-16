package com.devinolabs.uap.integrations.domain;

/**
 * V5 OS hubs plus V9 wearable/direct channels (ADR-055).
 *
 * <p>OS hubs and BLE_DIRECT are client upload-only. OAuth wearables may pull only when
 * CERTIFIED and enabled; V9 adapters fail live pull honestly without credentials.
 */
public enum HealthProviderKey {
	APPLE_HEALTHKIT,
	HEALTH_CONNECT,
	GARMIN,
	FITBIT,
	WHOOP,
	OURA,
	POLAR,
	BLE_DIRECT;

	public boolean isOsHub() {
		return this == APPLE_HEALTHKIT || this == HEALTH_CONNECT;
	}

	public boolean isDirectSensorChannel() {
		return this == BLE_DIRECT;
	}

	public boolean isOauthWearable() {
		return this == GARMIN
				|| this == FITBIT
				|| this == WHOOP
				|| this == OURA
				|| this == POLAR;
	}

	public boolean isClientUploadOnly() {
		return isOsHub() || isDirectSensorChannel();
	}
}
