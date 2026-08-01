package com.devinolabs.uap.training.domain;

/**
 * Completeness of a recovery check-in. COMPLETE when all required wellness ratings are present
 * (fatigue, muscleSoreness, stress, mood, motivation). Sleep fields remain optional.
 */
public enum RecoveryCheckInCompleteness {

	COMPLETE,
	PARTIAL

}
