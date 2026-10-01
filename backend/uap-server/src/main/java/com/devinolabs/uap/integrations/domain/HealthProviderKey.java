package com.devinolabs.uap.integrations.domain;

/**
 * V5 OS health hubs only (ADR-046 Connected Athlete MVP).
 *
 * <p>OS hubs are client upload-only: evidence reaches the server via evidence-batch ingest,
 * not server-side provider pull.
 */
public enum HealthProviderKey {
	APPLE_HEALTHKIT,
	HEALTH_CONNECT;

	/** True for Apple HealthKit and Health Connect (upload-only OS hubs). */
	public boolean isOsHub() {
		return switch (this) {
			case APPLE_HEALTHKIT, HEALTH_CONNECT -> true;
		};
	}
}
