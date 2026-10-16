package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.devinolabs.uap.integrations.api.evidence.FusedEvidenceView;
import com.devinolabs.uap.integrations.api.evidence.WearableSignalRules;

/**
 * V9 approved-signal gate. Does not rewrite READINESS_V1. Athlete-entered recovery wins.
 */
public final class WearableReadinessPolicy {

	private WearableReadinessPolicy() {
	}

	public static List<FusedEvidenceView> approvedForReadiness(
			List<FusedEvidenceView> fused,
			Instant now,
			boolean athleteEnteredRecoveryPresent) {
		Objects.requireNonNull(now, "now must not be null");
		if (fused == null || fused.isEmpty() || athleteEnteredRecoveryPresent) {
			return List.of();
		}
		Map<String, FusedEvidenceView> unique = new LinkedHashMap<>();
		for (FusedEvidenceView item : fused) {
			if (item == null || item.signalFamily() == null) {
				continue;
			}
			if (!WearableSignalRules.meetsReadinessQuality(item.quality())) {
				continue;
			}
			if (WearableSignalRules.isStale(item.signalFamily(), item.observedAt(), now)) {
				continue;
			}
			String key = item.signalFamily() + "|" + item.signalType();
			unique.putIfAbsent(key, item);
		}
		return List.copyOf(new ArrayList<>(unique.values()));
	}

	public static boolean missingWearableIsPenalty() {
		return false;
	}
}
