package com.devinolabs.uap.training.domain;

/**
 * Whether a TRAINING_RECOMMENDATION_V1 adjustment can be concretely expressed by the
 * current adaptation engine (exercise substitution only).
 */
public enum TrainingAdjustmentApplicability {
	CONCRETELY_APPLICABLE,
	CONTEXT_ONLY,
	NOT_APPLICABLE
}
