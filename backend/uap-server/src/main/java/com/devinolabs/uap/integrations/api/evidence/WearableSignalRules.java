package com.devinolabs.uap.integrations.api.evidence;

import java.time.Instant;

import com.devinolabs.uap.integrations.domain.EvidenceQuality;
import com.devinolabs.uap.integrations.domain.EvidenceQualityPolicy;
import com.devinolabs.uap.integrations.domain.SignalFamily;

/**
 * Published wearable consumption rules. Training may use this type; it must not import
 * undocumented vendor payloads.
 */
public final class WearableSignalRules {

	private WearableSignalRules() {
	}

	public static boolean meetsReadinessQuality(String quality) {
		return EvidenceQuality.parseOrUnknown(quality).meetsReadinessGate();
	}

	public static boolean isStale(String signalFamily, Instant observedAt, Instant now) {
		if (signalFamily == null || signalFamily.isBlank()) {
			return true;
		}
		try {
			return EvidenceQualityPolicy.isStale(SignalFamily.valueOf(signalFamily), observedAt, now);
		}
		catch (IllegalArgumentException ex) {
			return true;
		}
	}
}
