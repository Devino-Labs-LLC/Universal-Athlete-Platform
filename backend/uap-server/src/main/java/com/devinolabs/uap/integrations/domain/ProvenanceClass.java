package com.devinolabs.uap.integrations.domain;

/**
 * Where a connected observation originated (ADR-047 / ADR-051 / ADR-055).
 */
public enum ProvenanceClass {
	CLIENT_DEVICE,
	OS_HUB,
	OAUTH_PROVIDER,
	DIRECT_SENSOR
}
