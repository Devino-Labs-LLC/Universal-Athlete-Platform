package com.devinolabs.uap.training.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Builds categorical TRAINING_RECOMMENDATION_V1 adjustments from action + limiting dimensions.
 */
public final class TrainingAdjustmentResolver {

	private TrainingAdjustmentResolver() {
	}

	public static List<TrainingRecommendationAdjustment> resolve(
			TrainingRecommendationAction overallAction,
			ReadinessBand readinessBand,
			List<ReadinessDimensionType> limitingDimensions) {
		Objects.requireNonNull(overallAction, "overallAction must not be null");
		Objects.requireNonNull(readinessBand, "readinessBand must not be null");
		Objects.requireNonNull(limitingDimensions, "limitingDimensions must not be null");

		if (overallAction == TrainingRecommendationAction.INSUFFICIENT_DATA
				|| overallAction == TrainingRecommendationAction.TRAINING_ALREADY_COMPLETED
				|| overallAction == TrainingRecommendationAction.NO_SCHEDULED_TRAINING) {
			return List.of();
		}

		Map<TrainingAdjustmentType, Draft> drafts = new EnumMap<>(TrainingAdjustmentType.class);

		if (overallAction == TrainingRecommendationAction.CONSIDER_RECOVERY_SESSION) {
			add(drafts, TrainingAdjustmentType.OPTIONAL_RECOVERY_FOCUS,
					TrainingRecommendationReasonCode.READINESS_LOW, null);
			return toOrderedAdjustments(drafts);
		}

		if (overallAction == TrainingRecommendationAction.PROCEED_AS_PLANNED
				|| readinessBand == ReadinessBand.HIGH) {
			add(drafts, TrainingAdjustmentType.PRESERVE_PLANNED_SESSION,
					TrainingRecommendationReasonCode.NO_ADJUSTMENT_REQUIRED, null);
			return toOrderedAdjustments(drafts);
		}

		if (readinessBand == ReadinessBand.MODERATE) {
			add(drafts, TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
					TrainingRecommendationReasonCode.READINESS_MODERATE, null);
		}
		else if (readinessBand == ReadinessBand.LOW) {
			add(drafts, TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
					TrainingRecommendationReasonCode.READINESS_LOW, null);
			add(drafts, TrainingAdjustmentType.REDUCE_INTENSITY,
					TrainingRecommendationReasonCode.READINESS_LOW, null);
		}

		for (ReadinessDimensionType dimension : limitingDimensions) {
			applyLimitingDimension(drafts, dimension);
		}

		return toOrderedAdjustments(drafts);
	}

	private static void applyLimitingDimension(
			Map<TrainingAdjustmentType, Draft> drafts,
			ReadinessDimensionType dimension) {
		switch (dimension) {
			case FATIGUE -> {
				add(drafts, TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
						TrainingRecommendationReasonCode.FATIGUE_LIMITING, dimension);
				add(drafts, TrainingAdjustmentType.REDUCE_INTENSITY,
						TrainingRecommendationReasonCode.FATIGUE_LIMITING, dimension);
			}
			case MUSCLE_SORENESS -> {
				add(drafts, TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
						TrainingRecommendationReasonCode.SORENESS_LIMITING, dimension);
				add(drafts, TrainingAdjustmentType.PREFER_LOWER_IMPACT_VARIATIONS,
						TrainingRecommendationReasonCode.SORENESS_LIMITING, dimension);
			}
			case STRESS -> {
				add(drafts, TrainingAdjustmentType.REDUCE_SESSION_DURATION,
						TrainingRecommendationReasonCode.STRESS_LIMITING, dimension);
				add(drafts, TrainingAdjustmentType.INCREASE_REST,
						TrainingRecommendationReasonCode.STRESS_LIMITING, dimension);
			}
			case MOTIVATION -> add(drafts, TrainingAdjustmentType.REDUCE_SESSION_DURATION,
					TrainingRecommendationReasonCode.MOTIVATION_LIMITING, dimension);
			case SLEEP_QUALITY -> {
				add(drafts, TrainingAdjustmentType.REDUCE_INTENSITY,
						TrainingRecommendationReasonCode.SLEEP_QUALITY_LIMITING, dimension);
				add(drafts, TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
						TrainingRecommendationReasonCode.SLEEP_QUALITY_LIMITING, dimension);
			}
			case SLEEP_DURATION -> {
				add(drafts, TrainingAdjustmentType.REDUCE_INTENSITY,
						TrainingRecommendationReasonCode.SLEEP_DURATION_LIMITING, dimension);
				add(drafts, TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
						TrainingRecommendationReasonCode.SLEEP_DURATION_LIMITING, dimension);
			}
			case MOOD -> {
				// MOOD limiting is explanatory only in V1 — no additional physical adjustment.
			}
			case TRAINING_LOAD_CONTEXT -> {
				// Context-only in READINESS_V1 — never limiting.
			}
		}
	}

	private static void add(
			Map<TrainingAdjustmentType, Draft> drafts,
			TrainingAdjustmentType type,
			TrainingRecommendationReasonCode reason,
			ReadinessDimensionType dimension) {
		Draft draft = drafts.computeIfAbsent(type, ignored -> new Draft(type));
		draft.reasons.add(reason);
		if (dimension != null) {
			draft.dimensions.add(dimension);
		}
	}

	private static List<TrainingRecommendationAdjustment> toOrderedAdjustments(
			Map<TrainingAdjustmentType, Draft> drafts) {
		List<Draft> ordered = drafts.values().stream()
				.sorted(Comparator.comparingInt((Draft d) -> d.type.priority())
						.thenComparing(d -> d.type.name()))
				.toList();
		List<TrainingRecommendationAdjustment> result = new ArrayList<>(ordered.size());
		int orderIndex = 0;
		for (Draft draft : ordered) {
			result.add(new TrainingRecommendationAdjustment(
					UUID.randomUUID(),
					draft.type,
					draft.type.priority(),
					List.copyOf(draft.reasons),
					List.copyOf(draft.dimensions),
					"training.recommendation.adjustment." + draft.type.name().toLowerCase(),
					orderIndex++));
		}
		return List.copyOf(result);
	}

	private static final class Draft {
		private final TrainingAdjustmentType type;
		private final Set<TrainingRecommendationReasonCode> reasons = new LinkedHashSet<>();
		private final Set<ReadinessDimensionType> dimensions = EnumSet.noneOf(ReadinessDimensionType.class);

		private Draft(TrainingAdjustmentType type) {
			this.type = type;
		}
	}

}
