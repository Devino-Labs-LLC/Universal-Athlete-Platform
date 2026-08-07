package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationReasonCode;
import com.devinolabs.uap.training.domain.TrainingRecommendationStatus;

public record DailyTrainingRecommendationSummary(
		UUID recommendationId,
		LocalDate stateDate,
		UUID dailyReadinessAssessmentId,
		UUID dailyAthleteStateSnapshotId,
		int dailyAthleteStateSnapshotVersion,
		boolean currentSnapshot,
		TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
		TrainingRecommendationAction overallAction,
		TrainingRecommendationStatus recommendationStatus,
		TrainingRecommendationReasonCode primaryReasonCode,
		int adjustmentCount,
		Instant generatedAt) {
}
