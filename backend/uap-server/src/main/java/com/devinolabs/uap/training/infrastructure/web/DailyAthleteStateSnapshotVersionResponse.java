package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotSummary;
import com.devinolabs.uap.training.domain.DailyAthleteStateCompleteness;
import com.devinolabs.uap.training.domain.DailyAthleteStateGenerationReason;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsCalculationVersion;

record DailyAthleteStateSnapshotVersionResponse(
		UUID snapshotId,
		LocalDate stateDate,
		int snapshotVersion,
		boolean current,
		Instant generatedAt,
		DailyAthleteStateGenerationReason generationReason,
		String sourceFingerprint,
		RecoveryAnalyticsCalculationVersion recoveryAnalyticsCalculationVersion,
		int baselineWindowDays,
		DailyAthleteStateCompleteness completeness) {

	static DailyAthleteStateSnapshotVersionResponse from(DailyAthleteStateSnapshotSummary summary) {
		return new DailyAthleteStateSnapshotVersionResponse(
				summary.snapshotId(),
				summary.stateDate(),
				summary.snapshotVersion(),
				summary.current(),
				summary.generatedAt(),
				summary.generationReason(),
				summary.sourceFingerprint(),
				summary.recoveryAnalyticsCalculationVersion(),
				summary.baselineWindowDays(),
				summary.completeness());
	}

}
