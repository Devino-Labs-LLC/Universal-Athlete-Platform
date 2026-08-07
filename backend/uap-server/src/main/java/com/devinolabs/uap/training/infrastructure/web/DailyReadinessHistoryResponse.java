package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyReadinessAssessmentSummary;
import com.devinolabs.uap.training.application.DailyReadinessHistoryPage;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDataSufficiency;
import com.devinolabs.uap.training.domain.ReadinessReasonCode;

record DailyReadinessHistoryResponse(
		List<DailyReadinessHistoryItemResponse> content,
		int page,
		int size,
		long totalElements) {

	static DailyReadinessHistoryResponse from(DailyReadinessHistoryPage page) {
		return new DailyReadinessHistoryResponse(
				page.content().stream().map(DailyReadinessHistoryItemResponse::from).toList(),
				page.page(),
				page.size(),
				page.totalElements());
	}

}

record DailyReadinessHistoryItemResponse(
		UUID assessmentId,
		LocalDate stateDate,
		UUID dailyAthleteStateSnapshotId,
		int dailyAthleteStateSnapshotVersion,
		boolean currentSnapshot,
		ReadinessAlgorithmVersion algorithmVersion,
		BigDecimal readinessScore,
		ReadinessBand readinessBand,
		ReadinessDataSufficiency dataSufficiency,
		ReadinessReasonCode summaryReasonCode,
		Instant assessedAt) {

	static DailyReadinessHistoryItemResponse from(DailyReadinessAssessmentSummary summary) {
		return new DailyReadinessHistoryItemResponse(
				summary.assessmentId(),
				summary.stateDate(),
				summary.dailyAthleteStateSnapshotId(),
				summary.dailyAthleteStateSnapshotVersion(),
				summary.currentSnapshot(),
				summary.algorithmVersion(),
				summary.readinessScore(),
				summary.readinessBand(),
				summary.dataSufficiency(),
				summary.summaryReasonCode(),
				summary.assessedAt());
	}

}
