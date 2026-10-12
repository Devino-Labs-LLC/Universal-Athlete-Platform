package com.devinolabs.uap.aicoach.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class CoachFreshnessTest {

	private final Instant now = Instant.parse("2026-10-08T15:00:00Z");

	@Test
	void nullSyncIsUnavailable() {
		assertEquals("UNAVAILABLE", CoachFreshness.label(null, now));
	}

	@Test
	void syncInsideTheWindowIsFresh() {
		assertEquals("FRESH", CoachFreshness.label(now.minus(java.time.Duration.ofHours(36)), now));
	}

	@Test
	void syncOlderThanTheWindowIsStale() {
		assertEquals("STALE", CoachFreshness.label(now.minus(java.time.Duration.ofHours(36)).minusSeconds(1), now));
	}

}
