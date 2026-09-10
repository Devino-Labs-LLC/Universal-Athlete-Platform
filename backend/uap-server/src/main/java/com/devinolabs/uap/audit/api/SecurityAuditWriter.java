package com.devinolabs.uap.audit.api;

/**
 * Cross-module append-only security audit writer (ADR-035).
 * Callers must not expose this as a public HTTP API.
 */
public interface SecurityAuditWriter {

	void append(SecurityAuditRecord record);

}
