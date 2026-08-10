package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.TrainingTodayDashboardResult;
import com.devinolabs.uap.training.domain.PersonalRecordMeasure;
import com.devinolabs.uap.training.domain.PersonalRecordType;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDataSufficiency;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationStatus;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalOrigin;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

record TrainingTodayDashboardResponse(
		LocalDate date,
		TrainingDashboardAthleteResponse athlete,
		TrainingDashboardRecoveryResponse recovery,
		TrainingDashboardAthleteStateResponse athleteState,
		TrainingDashboardReadinessResponse readiness,
		TrainingDashboardRecommendationResponse recommendation,
		TrainingDashboardTrainingResponse training,
		TrainingDashboardLoadResponse trainingLoad,
		TrainingDashboardAdaptationResponse adaptation,
		List<TrainingDashboardPersonalRecordResponse> recentPerformance,
		TrainingDashboardActionsResponse actions) {

	static TrainingTodayDashboardResponse from(TrainingTodayDashboardResult result) {
		return new TrainingTodayDashboardResponse(
				result.date(),
				new TrainingDashboardAthleteResponse(
						result.athlete().athleteId(),
						result.athlete().displayName()),
				TrainingDashboardRecoveryResponse.from(result.recovery()),
				TrainingDashboardAthleteStateResponse.from(result.athleteState()),
				TrainingDashboardReadinessResponse.from(result.readiness()),
				TrainingDashboardRecommendationResponse.from(result.recommendation()),
				TrainingDashboardTrainingResponse.from(result.training()),
				TrainingDashboardLoadResponse.from(result.trainingLoad()),
				TrainingDashboardAdaptationResponse.from(result.adaptation()),
				result.recentPerformance().stream()
						.map(TrainingDashboardPersonalRecordResponse::from)
						.toList(),
				TrainingDashboardActionsResponse.from(result.actions()));
	}

}

record TrainingDashboardAthleteResponse(
		UUID athleteId,
		String displayName) {
}

record TrainingDashboardRecoveryResponse(
		boolean checkInPresent,
		UUID recoveryCheckInId,
		RecoveryCheckInCompleteness completeness,
		boolean discomfortPresent,
		Integer fatigue,
		Integer muscleSoreness,
		Integer stress,
		Integer mood,
		Integer motivation,
		Integer sleepDurationMinutes,
		Integer sleepQuality) {

	static TrainingDashboardRecoveryResponse from(TrainingTodayDashboardResult.RecoverySection section) {
		return new TrainingDashboardRecoveryResponse(
				section.checkInPresent(),
				section.recoveryCheckInId(),
				section.completeness(),
				section.discomfortPresent(),
				section.fatigue(),
				section.muscleSoreness(),
				section.stress(),
				section.mood(),
				section.motivation(),
				section.sleepDurationMinutes(),
				section.sleepQuality());
	}
}

record TrainingDashboardAthleteStateResponse(
		boolean snapshotPresent,
		UUID dailyAthleteStateSnapshotId,
		Integer snapshotVersion) {

	static TrainingDashboardAthleteStateResponse from(TrainingTodayDashboardResult.AthleteStateSection section) {
		return new TrainingDashboardAthleteStateResponse(
				section.snapshotPresent(),
				section.dailyAthleteStateSnapshotId(),
				section.snapshotVersion());
	}
}

record TrainingDashboardReadinessResponse(
		boolean readinessPresent,
		UUID readinessAssessmentId,
		BigDecimal readinessScore,
		ReadinessBand readinessBand,
		ReadinessDataSufficiency dataSufficiency,
		List<ReadinessDimensionType> limitingDimensions) {

	static TrainingDashboardReadinessResponse from(TrainingTodayDashboardResult.ReadinessSection section) {
		return new TrainingDashboardReadinessResponse(
				section.readinessPresent(),
				section.readinessAssessmentId(),
				section.readinessScore(),
				section.readinessBand(),
				section.dataSufficiency(),
				section.limitingDimensions());
	}
}

record TrainingDashboardRecommendationResponse(
		boolean recommendationPresent,
		UUID recommendationId,
		TrainingRecommendationAction overallAction,
		TrainingRecommendationStatus recommendationStatus,
		List<TrainingAdjustmentType> adjustmentTypes) {

	static TrainingDashboardRecommendationResponse from(TrainingTodayDashboardResult.RecommendationSection section) {
		return new TrainingDashboardRecommendationResponse(
				section.recommendationPresent(),
				section.recommendationId(),
				section.overallAction(),
				section.recommendationStatus(),
				section.adjustmentTypes());
	}
}

record TrainingDashboardTrainingResponse(
		int scheduledOccurrenceCount,
		int modifiableOccurrenceCount,
		int completedOccurrenceCount,
		int inProgressOccurrenceCount,
		List<TrainingDashboardOccurrenceResponse> occurrences,
		TrainingDashboardOccurrenceResponse primaryOccurrence) {

	static TrainingDashboardTrainingResponse from(TrainingTodayDashboardResult.TrainingSection section) {
		return new TrainingDashboardTrainingResponse(
				section.scheduledOccurrenceCount(),
				section.modifiableOccurrenceCount(),
				section.completedOccurrenceCount(),
				section.inProgressOccurrenceCount(),
				section.occurrences().stream().map(TrainingDashboardOccurrenceResponse::from).toList(),
				section.primaryOccurrence() == null
						? null
						: TrainingDashboardOccurrenceResponse.from(section.primaryOccurrence()));
	}
}

record TrainingDashboardOccurrenceResponse(
		UUID occurrenceId,
		UUID trainingPlanId,
		UUID workoutDayId,
		String trainingPlanName,
		String workoutDayName,
		WorkoutOccurrenceStatus status,
		LocalDate scheduledDate,
		int exerciseCount,
		int completedExerciseCount,
		Instant startedAt,
		Instant completedAt,
		UUID plannedEnvironmentId,
		String plannedEnvironmentName,
		UUID actualEnvironmentId,
		String actualEnvironmentName,
		WorkoutFeasibilityStatus feasibilityStatus,
		UUID activeAdaptationProposalId) {

	static TrainingDashboardOccurrenceResponse from(TrainingTodayDashboardResult.OccurrenceSummary summary) {
		return new TrainingDashboardOccurrenceResponse(
				summary.occurrenceId(),
				summary.trainingPlanId(),
				summary.workoutDayId(),
				summary.trainingPlanName(),
				summary.workoutDayName(),
				summary.status(),
				summary.scheduledDate(),
				summary.exerciseCount(),
				summary.completedExerciseCount(),
				summary.startedAt(),
				summary.completedAt(),
				summary.plannedEnvironmentId(),
				summary.plannedEnvironmentName(),
				summary.actualEnvironmentId(),
				summary.actualEnvironmentName(),
				summary.feasibilityStatus(),
				summary.activeAdaptationProposalId());
	}
}

record TrainingDashboardLoadResponse(
		boolean loadPresent,
		long occurrenceCount,
		long ratedOccurrenceCount,
		long unratedOccurrenceCount,
		long completedExerciseCount,
		long completedSetCount,
		BigDecimal totalVolumeKilograms,
		long totalDurationSeconds,
		BigDecimal totalDistanceMeters,
		BigDecimal totalSessionRpeLoad,
		BigDecimal averageSessionRpe) {

	static TrainingDashboardLoadResponse from(TrainingTodayDashboardResult.TrainingLoadSection section) {
		return new TrainingDashboardLoadResponse(
				section.loadPresent(),
				section.occurrenceCount(),
				section.ratedOccurrenceCount(),
				section.unratedOccurrenceCount(),
				section.completedExerciseCount(),
				section.completedSetCount(),
				section.totalVolumeKilograms(),
				section.totalDurationSeconds(),
				section.totalDistanceMeters(),
				section.totalSessionRpeLoad(),
				section.averageSessionRpe());
	}
}

record TrainingDashboardAdaptationResponse(
		boolean activeProposalPresent,
		UUID adaptationProposalId,
		WorkoutAdaptationProposalStatus status,
		WorkoutAdaptationProposalOrigin origin,
		int unresolvedCount,
		UUID occurrenceId) {

	static TrainingDashboardAdaptationResponse from(TrainingTodayDashboardResult.AdaptationSection section) {
		return new TrainingDashboardAdaptationResponse(
				section.activeProposalPresent(),
				section.adaptationProposalId(),
				section.status(),
				section.origin(),
				section.unresolvedCount(),
				section.occurrenceId());
	}
}

record TrainingDashboardPersonalRecordResponse(
		UUID personalRecordId,
		String exerciseName,
		PersonalRecordType recordType,
		String recordQualifier,
		BigDecimal normalizedValue,
		PersonalRecordMeasure normalizedUnit,
		Instant achievedAt,
		LocalDate scheduledDate,
		UUID sourceOccurrenceId) {

	static TrainingDashboardPersonalRecordResponse from(TrainingTodayDashboardResult.PersonalRecordBrief brief) {
		return new TrainingDashboardPersonalRecordResponse(
				brief.personalRecordId(),
				brief.exerciseName(),
				brief.recordType(),
				brief.recordQualifier(),
				brief.normalizedValue(),
				brief.normalizedUnit(),
				brief.achievedAt(),
				brief.scheduledDate(),
				brief.sourceOccurrenceId());
	}
}

record TrainingDashboardActionsResponse(
		TrainingClientActionFlagResponse canCreateRecoveryCheckIn,
		TrainingClientActionFlagResponse canUpdateRecoveryCheckIn,
		TrainingClientActionFlagResponse canGenerateAthleteStateSnapshot,
		TrainingClientActionFlagResponse canGenerateReadinessAssessment,
		TrainingClientActionFlagResponse canGenerateTrainingRecommendation,
		TrainingClientActionFlagResponse canGenerateAdaptationProposal,
		TrainingClientActionFlagResponse canStartWorkout,
		TrainingClientActionFlagResponse canContinueWorkout,
		TrainingClientActionFlagResponse canSubmitSessionEffort) {

	static TrainingDashboardActionsResponse from(
			com.devinolabs.uap.training.application.TrainingTodayDashboardActionsResult actions) {
		return new TrainingDashboardActionsResponse(
				TrainingClientActionFlagResponse.from(actions.canCreateRecoveryCheckIn()),
				TrainingClientActionFlagResponse.from(actions.canUpdateRecoveryCheckIn()),
				TrainingClientActionFlagResponse.from(actions.canGenerateAthleteStateSnapshot()),
				TrainingClientActionFlagResponse.from(actions.canGenerateReadinessAssessment()),
				TrainingClientActionFlagResponse.from(actions.canGenerateTrainingRecommendation()),
				TrainingClientActionFlagResponse.from(actions.canGenerateAdaptationProposal()),
				TrainingClientActionFlagResponse.from(actions.canStartWorkout()),
				TrainingClientActionFlagResponse.from(actions.canContinueWorkout()),
				TrainingClientActionFlagResponse.from(actions.canSubmitSessionEffort()));
	}
}
