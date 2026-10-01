package com.devinolabs.uap.integrations.domain;

/**
 * Per-attempt sync orchestration status (ADR-048). Independent of connection lifecycle.
 */
public enum SyncRunStatus {
	REQUESTED,
	RUNNING,
	SUCCEEDED,
	PARTIAL,
	FAILED
}
