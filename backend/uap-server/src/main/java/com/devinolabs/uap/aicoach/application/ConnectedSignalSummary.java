package com.devinolabs.uap.aicoach.application;

import java.time.Instant;
import java.util.List;

public record ConnectedSignalSummary(
		String provider,
		String lifecycleState,
		String freshness,
		Instant lastSuccessfulSyncAt,
		Instant newestObservedAt,
		int observationCount,
		List<String> signalFamilies,
		String quality,
		boolean conflictingSources,
		boolean unavailable) {
}
