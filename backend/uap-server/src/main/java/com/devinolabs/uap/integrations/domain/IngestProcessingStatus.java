package com.devinolabs.uap.integrations.domain;

/**
 * Integrations-scoped inbox status (ADR-051). Not billing_provider_events.
 */
public enum IngestProcessingStatus {
	RECEIVED,
	PROCESSED,
	IGNORED,
	FAILED
}
