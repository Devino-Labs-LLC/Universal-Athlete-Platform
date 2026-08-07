package com.devinolabs.uap.training.domain;

import java.util.List;
import java.util.Objects;

/**
 * Provenance linking a proposal to the immutable recommendation/assessment/snapshot chain.
 */
public record WorkoutAdaptationRecommendationContext(
		DailyTrainingRecommendationId dailyTrainingRecommendationId,
		DailyReadinessAssessmentId dailyReadinessAssessmentId,
		DailyAthleteStateSnapshotId dailyAthleteStateSnapshotId,
		TrainingRecommendationAlgorithmVersion trainingRecommendationAlgorithmVersion,
		TrainingRecommendationAction recommendationOverallAction,
		ReadinessBand recommendationReadinessBand,
		List<WorkoutAdaptationRecommendationAdjustmentSnapshot> adjustments) {

	public WorkoutAdaptationRecommendationContext {
		Objects.requireNonNull(dailyTrainingRecommendationId, "dailyTrainingRecommendationId must not be null");
		Objects.requireNonNull(dailyReadinessAssessmentId, "dailyReadinessAssessmentId must not be null");
		Objects.requireNonNull(dailyAthleteStateSnapshotId, "dailyAthleteStateSnapshotId must not be null");
		Objects.requireNonNull(
				trainingRecommendationAlgorithmVersion, "trainingRecommendationAlgorithmVersion must not be null");
		Objects.requireNonNull(recommendationOverallAction, "recommendationOverallAction must not be null");
		Objects.requireNonNull(recommendationReadinessBand, "recommendationReadinessBand must not be null");
		Objects.requireNonNull(adjustments, "adjustments must not be null");
		adjustments = List.copyOf(adjustments);
	}

	public boolean preferLowerImpactVariations() {
		return adjustments.stream().anyMatch(adjustment ->
				adjustment.trainingAdjustmentType() == TrainingAdjustmentType.PREFER_LOWER_IMPACT_VARIATIONS
						&& adjustment.applicability() == TrainingAdjustmentApplicability.CONCRETELY_APPLICABLE);
	}

	public static WorkoutAdaptationRecommendationContext from(
			DailyTrainingRecommendation recommendation,
			DailyReadinessAssessment assessment) {
		Objects.requireNonNull(recommendation, "recommendation must not be null");
		Objects.requireNonNull(assessment, "assessment must not be null");
		List<WorkoutAdaptationRecommendationAdjustmentSnapshot> snapshots = recommendation.adjustments().stream()
				.map(WorkoutAdaptationRecommendationAdjustmentSnapshot::from)
				.toList();
		return new WorkoutAdaptationRecommendationContext(
				recommendation.id(),
				recommendation.dailyReadinessAssessmentId(),
				recommendation.dailyAthleteStateSnapshotId(),
				recommendation.recommendationAlgorithmVersion(),
				recommendation.overallAction(),
				assessment.readinessBand(),
				snapshots);
	}

}
