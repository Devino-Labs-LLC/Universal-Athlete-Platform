package com.devinolabs.uap.consent.domain;

/**
 * Sensitive sharing scopes. Roster-safe identity is membership effect, not a ConsentGrant scope.
 */
public enum ConsentScope {
	AVAILABILITY,
	READINESS_CATEGORY,
	READINESS_SCORE,
	LIMITING_DIMENSIONS,
	RECOVERY_CHECK_IN_DETAIL,
	TRAINING_ADHERENCE,
	PERFORMANCE_HISTORY,
	TRAINING_COLLABORATION,
	EXPORT
}
