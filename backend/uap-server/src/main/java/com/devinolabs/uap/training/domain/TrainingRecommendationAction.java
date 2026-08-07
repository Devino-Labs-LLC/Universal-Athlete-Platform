package com.devinolabs.uap.training.domain;

/**
 * Overall daily training action suggested by TRAINING_RECOMMENDATION_V1.
 * Guidance only — never auto-mutates workouts.
 */
public enum TrainingRecommendationAction {
	PROCEED_AS_PLANNED,
	MODIFY_SESSION,
	CONSIDER_RECOVERY_SESSION,
	NO_SCHEDULED_TRAINING,
	INSUFFICIENT_DATA,
	TRAINING_ALREADY_COMPLETED
}
