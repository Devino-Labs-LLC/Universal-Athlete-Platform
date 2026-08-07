package com.devinolabs.uap.training.domain;

import java.util.Objects;

/**
 * Maps TRAINING_RECOMMENDATION_V1 adjustment types to adaptation-engine applicability.
 */
public final class TrainingAdjustmentApplicabilityResolver {

	private TrainingAdjustmentApplicabilityResolver() {
	}

	public static TrainingAdjustmentApplicability resolve(TrainingAdjustmentType type) {
		return switch (Objects.requireNonNull(type, "type must not be null")) {
			case PREFER_LOWER_IMPACT_VARIATIONS, PREFER_EQUIPMENT_COMPATIBLE_VARIATIONS ->
					TrainingAdjustmentApplicability.CONCRETELY_APPLICABLE;
			case REDUCE_TOTAL_VOLUME, REDUCE_INTENSITY, REDUCE_SESSION_DURATION, INCREASE_REST,
					OPTIONAL_RECOVERY_FOCUS ->
					TrainingAdjustmentApplicability.CONTEXT_ONLY;
			case PRESERVE_PLANNED_SESSION, NO_ADJUSTMENT ->
					TrainingAdjustmentApplicability.NOT_APPLICABLE;
		};
	}

}
