package com.devinolabs.uap.aicoach.application;

import java.time.Duration;
import java.time.Instant;

final class CoachFreshness {

	static final Duration EVIDENCE_FRESH_WINDOW = Duration.ofHours(36);

	private CoachFreshness() {
	}

	static String label(Instant lastSuccessfulSyncAt, Instant now) {
		if (lastSuccessfulSyncAt == null) {
			return "UNAVAILABLE";
		}
		if (lastSuccessfulSyncAt.isAfter(now)) {
			return "FRESH";
		}
		return lastSuccessfulSyncAt.isBefore(now.minus(EVIDENCE_FRESH_WINDOW)) ? "STALE" : "FRESH";
	}

}
