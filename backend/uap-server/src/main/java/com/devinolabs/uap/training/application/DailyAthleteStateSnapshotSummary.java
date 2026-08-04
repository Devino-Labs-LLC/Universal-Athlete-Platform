package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.devinolabs.uap.training.domain.DailyAthleteStateCompleteness;
import com.devinolabs.uap.training.domain.DailyAthleteStateGenerationReason;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsCalculationVersion;

public record DailyAthleteStateSnapshotSummary(
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
}
