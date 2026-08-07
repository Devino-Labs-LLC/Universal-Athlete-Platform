package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic TRAINING_RECOMMENDATION_V1 calculator.
 * Consumes only an immutable readiness assessment and its source snapshot.
 */
public final class TrainingRecommendationCalculator {

	public static final TrainingRecommendationAlgorithmVersion ALGORITHM_VERSION =
			TrainingRecommendationAlgorithmVersion.TRAINING_RECOMMENDATION_V1;

	private TrainingRecommendationCalculator() {
	}

	public static CalculationResult calculate(
			DailyReadinessAssessment assessment,
			DailyAthleteStateSnapshot snapshot,
			Clock clock) {
		Objects.requireNonNull(assessment, "assessment must not be null");
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		if (!assessment.dailyAthleteStateSnapshotId().equals(snapshot.id())) {
			throw new IllegalArgumentException("Assessment snapshot id does not match provided snapshot");
		}
		if (!assessment.athleteId().equals(snapshot.athleteId())) {
			throw new IllegalArgumentException("Assessment athlete id does not match provided snapshot");
		}

		List<DailyAthleteStateScheduledOccurrenceSnapshot> occurrences = snapshot.scheduledOccurrences().stream()
				.sorted(Comparator.comparingInt(DailyAthleteStateScheduledOccurrenceSnapshot::orderIndex)
						.thenComparing(DailyAthleteStateScheduledOccurrenceSnapshot::occurrenceId))
				.toList();

		TrainingRecommendationActionResolver.ResolvedAction resolved =
				TrainingRecommendationActionResolver.resolve(assessment.readinessBand(), occurrences);

		List<TrainingRecommendationAdjustment> adjustments = TrainingAdjustmentResolver.resolve(
				resolved.overallAction(),
				assessment.readinessBand(),
				assessment.limitingDimensions());

		TrainingRecommendationReasonCode primaryReason = resolved.primaryReasonCode();

		List<TrainingRecommendationOccurrenceContext> occurrenceContexts = new ArrayList<>();
		int order = 0;
		int modifiableCount = 0;
		for (DailyAthleteStateScheduledOccurrenceSnapshot occurrence : occurrences) {
			boolean modifiable = TrainingRecommendationOccurrenceContext.isModifiable(occurrence.occurrenceStatus());
			if (modifiable) {
				modifiableCount++;
			}
			occurrenceContexts.add(new TrainingRecommendationOccurrenceContext(
					occurrence.occurrenceId(),
					occurrence.trainingPlanId(),
					occurrence.workoutDayId(),
					occurrence.occurrenceStatus(),
					modifiable,
					occurrence.plannedEnvironmentNameSnapshot(),
					occurrence.actualEnvironmentNameSnapshot(),
					order++));
		}

		boolean scheduledPresent = occurrences.stream()
				.anyMatch(o -> o.occurrenceStatus() != WorkoutOccurrenceStatus.CANCELLED);
		Instant generatedAt = Instant.now(clock);

		return new CalculationResult(
				resolved.overallAction(),
				resolved.status(),
				primaryReason,
				scheduledPresent,
				(int) snapshot.scheduledOccurrenceCount(),
				modifiableCount,
				adjustments,
				assessment.limitingDimensions(),
				List.copyOf(occurrenceContexts),
				generatedAt);
	}

	public record CalculationResult(
			TrainingRecommendationAction overallAction,
			TrainingRecommendationStatus recommendationStatus,
			TrainingRecommendationReasonCode primaryReasonCode,
			boolean scheduledTrainingPresent,
			int scheduledOccurrenceCount,
			int modifiableScheduledOccurrenceCount,
			List<TrainingRecommendationAdjustment> adjustments,
			List<ReadinessDimensionType> limitingDimensions,
			List<TrainingRecommendationOccurrenceContext> occurrenceContexts,
			Instant generatedAt) {
	}

}
