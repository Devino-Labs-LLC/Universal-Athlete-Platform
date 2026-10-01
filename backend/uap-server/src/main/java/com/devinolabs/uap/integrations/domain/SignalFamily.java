package com.devinolabs.uap.integrations.domain;

/**
 * V5A store-capable signal families (ADR-047). Body metrics and proprietary recovery stay deferred.
 */
public enum SignalFamily {
	SLEEP,
	ACTIVITY,
	HEART,
	HRV,
	WORKOUT
}
