package com.devinolabs.uap.integrations.api.connections;

import java.time.Instant;
import java.util.UUID;

/**
 * Published sync-run status for an athlete-owned connection (no provider payloads).
 */
public record SyncRunView(
		UUID syncRunId,
		UUID connectionId,
		String status,
		Instant requestedAt,
		Instant startedAt,
		Instant finishedAt,
		String errorCode,
		int recordsAccepted,
		int recordsRejected) {
}
