package com.devinolabs.uap.integrations.domain;

/**
 * Provider-neutral sensor families. Not every family is a readiness input (ADR-047 / ADR-055).
 */
public enum SignalFamily {
	SLEEP,
	ACTIVITY,
	HEART,
	HRV,
	WORKOUT,
	POWER,
	CADENCE,
	RESPIRATORY,
	TEMPERATURE
}
