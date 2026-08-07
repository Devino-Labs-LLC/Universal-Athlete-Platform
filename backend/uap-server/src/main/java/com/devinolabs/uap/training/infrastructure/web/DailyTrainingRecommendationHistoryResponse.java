package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyTrainingRecommendationHistoryPage;
import com.devinolabs.uap.training.application.DailyTrainingRecommendationSummary;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationReasonCode;
import com.devinolabs.uap.training.domain.TrainingRecommendationStatus;

record DailyTrainingRecommendationHistoryResponse(
		List<DailyTrainingRecommendationHistoryItemResponse> content,
		int page,
		int size,
		long totalElements) {

	static DailyTrainingRecommendationHistoryResponse from(DailyTrainingRecommendationHistoryPage page) {
		return new DailyTrainingRecommendationHistoryResponse(
				page.content().stream().map(DailyTrainingRecommendationHistoryItemResponse::from).toList(),
				page.page(),
				page.size(),
				page.totalElements());
	}

}

record DailyTrainingRecommendationHistoryItemResponse(
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

	static DailyTrainingRecommendationHistoryItemResponse from(DailyTrainingRecommendationSummary summary) {
		return new DailyTrainingRecommendationHistoryItemResponse(
				summary.recommendationId(),
				summary.stateDate(),
				summary.dailyReadinessAssessmentId(),
				summary.dailyAthleteStateSnapshotId(),
				summary.dailyAthleteStateSnapshotVersion(),
				summary.currentSnapshot(),
				summary.recommendationAlgorithmVersion(),
				summary.overallAction(),
				summary.recommendationStatus(),
				summary.primaryReasonCode(),
				summary.adjustmentCount(),
				summary.generatedAt());
	}

}
