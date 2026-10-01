package com.devinolabs.uap.integrations.api.connections;

import java.util.UUID;

/**
 * Result of an idempotent evidence-batch upload (no readiness side effects).
 */
public record EvidenceBatchResultView(
		UUID requestId,
		UUID syncRunId,
		int acceptedCount,
		int rejectedCount,
		boolean replayed) {
}
