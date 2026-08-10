package com.devinolabs.uap.training.application;

/**
 * UI convenience action flags for the today dashboard. Never treated as authorization.
 */
public record TrainingTodayDashboardActionsResult(
		TrainingClientActionFlag canCreateRecoveryCheckIn,
		TrainingClientActionFlag canUpdateRecoveryCheckIn,
		TrainingClientActionFlag canGenerateAthleteStateSnapshot,
		TrainingClientActionFlag canGenerateReadinessAssessment,
		TrainingClientActionFlag canGenerateTrainingRecommendation,
		TrainingClientActionFlag canGenerateAdaptationProposal,
		TrainingClientActionFlag canStartWorkout,
		TrainingClientActionFlag canContinueWorkout,
		TrainingClientActionFlag canSubmitSessionEffort) {
}
