package com.devinolabs.uap.integrations.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/**
 * Derives bounded quality. Never claims medical-grade accuracy (ADR-055).
 */
public final class EvidenceQualityPolicy {

	private EvidenceQualityPolicy() {
	}

	public static Duration staleAfter(SignalFamily family) {
		return switch (family) {
			case HEART, HRV, POWER, CADENCE -> Duration.ofHours(24);
			case SLEEP -> Duration.ofHours(36);
			case ACTIVITY, WORKOUT, RESPIRATORY, TEMPERATURE -> Duration.ofHours(48);
		};
	}

	public static boolean isStale(SignalFamily family, Instant observedAt, Instant now) {
		if (observedAt == null || now == null) {
			return true;
		}
		return observedAt.isBefore(now.minus(staleAfter(family)));
	}

	public static EvidenceQuality derive(
			ProvenanceClass provenance,
			EvidenceQuality supplied,
			SignalFamily family,
			Instant observedAt,
			Instant now,
			Set<DeviceCapability> capabilities) {
		EvidenceQuality base = supplied == null || supplied == EvidenceQuality.UNKNOWN
				? provenanceDefault(provenance)
				: supplied;
		boolean capabilityOk = capabilities == null || capabilities.isEmpty()
				|| capabilities.stream().anyMatch(capability -> capability.supports(family));
		if (!capabilityOk) {
			return EvidenceQuality.LOW;
		}
		if (isStale(family, observedAt, now)) {
			return EvidenceQuality.LOW;
		}
		return base;
	}

	private static EvidenceQuality provenanceDefault(ProvenanceClass provenance) {
		if (provenance == null) {
			return EvidenceQuality.UNKNOWN;
		}
		return switch (provenance) {
			case DIRECT_SENSOR -> EvidenceQuality.HIGH;
			case CLIENT_DEVICE, OAUTH_PROVIDER -> EvidenceQuality.MEDIUM;
			case OS_HUB -> EvidenceQuality.MEDIUM;
		};
	}
}
