package com.devinolabs.uap.training.domain;

import java.util.List;
import java.util.Objects;

/**
 * Resolves TRAINING_RECOMMENDATION_V1 overall action from readiness band + schedule facts.
 */
public final class TrainingRecommendationActionResolver {

	private TrainingRecommendationActionResolver() {
	}

	public static ResolvedAction resolve(
			ReadinessBand readinessBand,
			List<DailyAthleteStateScheduledOccurrenceSnapshot> occurrences) {
		Objects.requireNonNull(readinessBand, "readinessBand must not be null");
		Objects.requireNonNull(occurrences, "occurrences must not be null");

		if (readinessBand == ReadinessBand.INSUFFICIENT_DATA) {
			return new ResolvedAction(
					TrainingRecommendationAction.INSUFFICIENT_DATA,
					TrainingRecommendationStatus.INSUFFICIENT_DATA,
					TrainingRecommendationReasonCode.READINESS_INSUFFICIENT);
		}

		int modifiableCount = (int) occurrences.stream()
				.filter(o -> TrainingRecommendationOccurrenceContext.isModifiable(o.occurrenceStatus()))
				.count();
		boolean allCompleted = !occurrences.isEmpty()
				&& occurrences.stream().allMatch(o -> o.occurrenceStatus() == WorkoutOccurrenceStatus.COMPLETED);
		boolean scheduledPresent = !occurrences.isEmpty()
				&& occurrences.stream().anyMatch(o -> o.occurrenceStatus() != WorkoutOccurrenceStatus.CANCELLED);

		if (allCompleted) {
			return new ResolvedAction(
					TrainingRecommendationAction.TRAINING_ALREADY_COMPLETED,
					TrainingRecommendationStatus.INFORMATIONAL,
					TrainingRecommendationReasonCode.TRAINING_ALREADY_COMPLETED);
		}

		if (!scheduledPresent || modifiableCount == 0) {
			return switch (readinessBand) {
				case HIGH, MODERATE -> new ResolvedAction(
						TrainingRecommendationAction.NO_SCHEDULED_TRAINING,
						TrainingRecommendationStatus.INFORMATIONAL,
						TrainingRecommendationReasonCode.NO_SCHEDULED_TRAINING);
				case LOW -> new ResolvedAction(
						TrainingRecommendationAction.CONSIDER_RECOVERY_SESSION,
						TrainingRecommendationStatus.ACTIONABLE,
						TrainingRecommendationReasonCode.READINESS_LOW);
				default -> new ResolvedAction(
						TrainingRecommendationAction.INSUFFICIENT_DATA,
						TrainingRecommendationStatus.INSUFFICIENT_DATA,
						TrainingRecommendationReasonCode.READINESS_INSUFFICIENT);
			};
		}

		return switch (readinessBand) {
			case HIGH -> new ResolvedAction(
					TrainingRecommendationAction.PROCEED_AS_PLANNED,
					TrainingRecommendationStatus.INFORMATIONAL,
					TrainingRecommendationReasonCode.READINESS_HIGH);
			case MODERATE -> new ResolvedAction(
					TrainingRecommendationAction.MODIFY_SESSION,
					TrainingRecommendationStatus.ACTIONABLE,
					TrainingRecommendationReasonCode.READINESS_MODERATE);
			case LOW -> new ResolvedAction(
					TrainingRecommendationAction.MODIFY_SESSION,
					TrainingRecommendationStatus.ACTIONABLE,
					TrainingRecommendationReasonCode.READINESS_LOW);
			default -> new ResolvedAction(
					TrainingRecommendationAction.INSUFFICIENT_DATA,
					TrainingRecommendationStatus.INSUFFICIENT_DATA,
					TrainingRecommendationReasonCode.READINESS_INSUFFICIENT);
		};
	}

	public record ResolvedAction(
			TrainingRecommendationAction overallAction,
			TrainingRecommendationStatus status,
			TrainingRecommendationReasonCode primaryReasonCode) {
	}

}
