package com.devinolabs.uap.training.domain;

/**
 * Source-availability label for a daily athlete-state snapshot.
 * Not readiness, coaching, or medical confidence.
 */
public enum DailyAthleteStateCompleteness {
	COMPLETE,
	PARTIAL,
	MINIMAL
}
