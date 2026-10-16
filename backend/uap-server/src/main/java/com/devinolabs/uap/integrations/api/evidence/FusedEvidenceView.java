package com.devinolabs.uap.integrations.api.evidence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Deterministic fused observation. Original evidence rows remain stored.
 */
public record FusedEvidenceView(
		UUID winningEvidenceId,
		UUID deviceId,
		String provider,
		String signalFamily,
		String signalType,
		BigDecimal valueNumeric,
		String unitCode,
		Instant observedAt,
		String provenanceClass,
		String quality,
		int fusionScore,
		List<UUID> consideredEvidenceIds) {
}
