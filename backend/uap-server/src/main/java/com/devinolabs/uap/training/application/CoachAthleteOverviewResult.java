package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Consent-aware coach athlete overview composition (Slice D).
 */
public record CoachAthleteOverviewResult(
		UUID teamId,
		UUID organizationId,
		UUID athleteId,
		UUID membershipId,
		String displayName,
		String role,
		LocalDate viewDate,
		Set<String> effectiveScopes,
		CoachOverviewSection<Void> availability,
		CoachOverviewSection<ReadinessCategoryData> readinessCategory,
		CoachOverviewSection<ReadinessScoreData> readinessScore,
		CoachOverviewSection<LimitingDimensionsData> limitingDimensions,
		CoachOverviewSection<RecoveryCheckInData> recoveryCheckIn,
		CoachOverviewSection<TrainingAdherenceData> trainingAdherence,
		CoachOverviewSection<PerformanceHistoryData> performanceHistory) {

	public record CoachOverviewSection<T>(CoachOverviewSectionStatus status, T data) {

		public static <T> CoachOverviewSection<T> notShared() {
			return new CoachOverviewSection<>(CoachOverviewSectionStatus.NOT_SHARED, null);
		}

		public static <T> CoachOverviewSection<T> noData() {
			return new CoachOverviewSection<>(CoachOverviewSectionStatus.NO_DATA, null);
		}

		public static <T> CoachOverviewSection<T> available(T data) {
			return new CoachOverviewSection<>(CoachOverviewSectionStatus.AVAILABLE, data);
		}
	}

	public record ReadinessCategoryData(String readinessBand, String dataSufficiency) {
	}

	public record ReadinessScoreData(
			BigDecimal readinessScore,
			String dataSufficiency,
			String summaryReasonCode) {
	}

	public record LimitingDimensionsData(List<String> limitingDimensions) {
	}

	public record RatingData(int value, String label) {
	}

	public record DiscomfortData(String bodyArea, String side, RatingData intensity, String notes, int orderIndex) {
	}

	public record RecoveryCheckInData(
			LocalDate checkInDate,
			RatingData sleepQuality,
			RatingData mood,
			RatingData fatigue,
			RatingData muscleSoreness,
			RatingData stress,
			String notes,
			String completeness,
			List<DiscomfortData> discomfortAreas) {
	}

	public record TrainingAdherenceData(
			int scheduledCount,
			int completedCount,
			int skippedCount,
			int inProgressCount,
			int cancelledCount) {
	}

	public record PerformanceHistoryEntry(
			String exerciseName,
			String recordType,
			String recordQualifier,
			Instant achievedAt,
			LocalDate scheduledDate) {
	}

	public record PerformanceHistoryData(List<PerformanceHistoryEntry> recentRecords) {
	}

}
