package com.devinolabs.uap.integrations.api.connections;

import java.time.Instant;
import java.util.UUID;

/**
 * Published athlete-owned connection status (no credentials / provider secrets).
 */
public record ConnectionView(
		UUID connectionId,
		String provider,
		String lifecycleState,
		boolean processConsentGranted,
		Instant processConsentGrantedAt,
		Instant connectedAt,
		Instant disconnectedAt,
		Instant lastSuccessfulSyncAt,
		Instant lastAttemptedSyncAt) {
}
