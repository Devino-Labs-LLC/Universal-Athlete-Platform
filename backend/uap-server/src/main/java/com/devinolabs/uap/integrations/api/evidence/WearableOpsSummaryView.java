package com.devinolabs.uap.integrations.api.evidence;

public record WearableOpsSummaryView(
		int syncAttempts,
		int syncSuccesses,
		int syncFailures,
		int evidenceAccepted,
		int evidenceRejected,
		int duplicates,
		int staleDeviceCount,
		String lastErrorCategory) {
}
