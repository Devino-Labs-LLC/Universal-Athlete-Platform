package com.devinolabs.uap.integrations.api.evidence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Provider-neutral connected evidence row for athlete self / later training consumers (V5A store-only).
 */
public record ConnectedEvidenceView(
		UUID evidenceId,
		UUID connectionId,
		String provider,
		String signalFamily,
		String signalType,
		String externalRecordId,
		BigDecimal valueNumeric,
		String valueText,
		String unitCode,
		Instant periodStart,
		Instant periodEnd,
		Instant observedAt,
		Instant providerUpdatedAt,
		Instant ingestedAt,
		UUID syncRunId,
		String provenanceClass,
		String sourceDeviceOrApp,
		String qualityCode,
		String status) {
}
