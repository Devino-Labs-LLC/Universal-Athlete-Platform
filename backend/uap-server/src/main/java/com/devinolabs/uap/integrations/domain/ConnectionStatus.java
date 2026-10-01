package com.devinolabs.uap.integrations.domain;

/**
 * Athlete ↔ provider/hub link lifecycle (ADR-048).
 */
public enum ConnectionStatus {
	DISCONNECTED,
	PENDING,
	CONNECTED,
	NEEDS_REAUTH,
	ERROR;

	/** MVP "active" set — at most one across providers per athlete. */
	public boolean isActive() {
		return this == PENDING || this == CONNECTED || this == NEEDS_REAUTH || this == ERROR;
	}
}
