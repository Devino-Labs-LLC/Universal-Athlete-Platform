package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrainingRecommendationCalculatorDomainTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC);
	private static final AthleteId ATHLETE_ID = AthleteId.of(UUID.randomUUID());
	private static final DailyAthleteStateSnapshotId SNAPSHOT_ID = DailyAthleteStateSnapshotId.generate();

	@Test
	void actionRulesCoverBandsAndScheduleStates() {
		assertThat(TrainingRecommendationActionResolver.resolve(
				ReadinessBand.HIGH, List.of(scheduledOccurrence())).overallAction())
				.isEqualTo(TrainingRecommendationAction.PROCEED_AS_PLANNED);
		assertThat(TrainingRecommendationActionResolver.resolve(
				ReadinessBand.MODERATE, List.of(scheduledOccurrence())).overallAction())
				.isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
		assertThat(TrainingRecommendationActionResolver.resolve(
				ReadinessBand.LOW, List.of(scheduledOccurrence())).overallAction())
				.isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
		assertThat(TrainingRecommendationActionResolver.resolve(
				ReadinessBand.LOW, List.of()).overallAction())
				.isEqualTo(TrainingRecommendationAction.CONSIDER_RECOVERY_SESSION);
		assertThat(TrainingRecommendationActionResolver.resolve(
				ReadinessBand.HIGH, List.of()).overallAction())
				.isEqualTo(TrainingRecommendationAction.NO_SCHEDULED_TRAINING);
		assertThat(TrainingRecommendationActionResolver.resolve(
				ReadinessBand.INSUFFICIENT_DATA, List.of(scheduledOccurrence())).overallAction())
				.isEqualTo(TrainingRecommendationAction.INSUFFICIENT_DATA);
		assertThat(TrainingRecommendationActionResolver.resolve(
				ReadinessBand.MODERATE, List.of(completedOccurrence())).overallAction())
				.isEqualTo(TrainingRecommendationAction.TRAINING_ALREADY_COMPLETED);
	}

	@Test
	void criticalLowScenarioProducesExpectedAdjustments() {
		DailyAthleteStateSnapshot snapshot = snapshot(List.of(scheduledOccurrence()), 1, 0, 0, 0, 0);
		DailyReadinessAssessment assessment = assessment(
				ReadinessBand.LOW,
				ReadinessScore.of(new BigDecimal("42.00")),
				List.of(
						ReadinessDimensionType.FATIGUE,
						ReadinessDimensionType.MOOD,
						ReadinessDimensionType.MOTIVATION));

		TrainingRecommendationCalculator.CalculationResult result =
				TrainingRecommendationCalculator.calculate(assessment, snapshot, CLOCK);

		assertThat(result.overallAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
		assertThat(result.recommendationStatus()).isEqualTo(TrainingRecommendationStatus.ACTIONABLE);
		assertThat(result.primaryReasonCode()).isEqualTo(TrainingRecommendationReasonCode.READINESS_LOW);
		assertThat(result.adjustments()).extracting(TrainingRecommendationAdjustment::type)
				.containsExactly(
						TrainingAdjustmentType.REDUCE_INTENSITY,
						TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
						TrainingAdjustmentType.REDUCE_SESSION_DURATION);
		assertThat(result.adjustments()).noneMatch(a ->
				a.type().name().contains("PERCENT") || a.explanationKey().contains("percent"));
		assertThat(result.modifiableScheduledOccurrenceCount()).isEqualTo(1);
	}

	@Test
	void highPreservesPlannedSessionWithoutDosage() {
		DailyAthleteStateSnapshot snapshot = snapshot(List.of(scheduledOccurrence()), 1, 0, 0, 0, 0);
		DailyReadinessAssessment assessment = assessment(
				ReadinessBand.HIGH,
				ReadinessScore.of(new BigDecimal("88.00")),
				List.of());
		TrainingRecommendationCalculator.CalculationResult result =
				TrainingRecommendationCalculator.calculate(assessment, snapshot, CLOCK);
		assertThat(result.overallAction()).isEqualTo(TrainingRecommendationAction.PROCEED_AS_PLANNED);
		assertThat(result.adjustments()).extracting(TrainingRecommendationAdjustment::type)
				.containsExactly(TrainingAdjustmentType.PRESERVE_PLANNED_SESSION);
	}

	@Test
	void insufficientReadinessYieldsEmptyAdjustments() {
		DailyAthleteStateSnapshot snapshot = snapshot(List.of(scheduledOccurrence()), 1, 0, 0, 0, 0);
		DailyReadinessAssessment assessment = assessment(
				ReadinessBand.INSUFFICIENT_DATA,
				null,
				List.of());
		TrainingRecommendationCalculator.CalculationResult result =
				TrainingRecommendationCalculator.calculate(assessment, snapshot, CLOCK);
		assertThat(result.overallAction()).isEqualTo(TrainingRecommendationAction.INSUFFICIENT_DATA);
		assertThat(result.adjustments()).isEmpty();
	}

	@Test
	void limitingDimensionRulesDeduplicateAndOrder() {
		List<TrainingRecommendationAdjustment> adjustments = TrainingAdjustmentResolver.resolve(
				TrainingRecommendationAction.MODIFY_SESSION,
				ReadinessBand.MODERATE,
				List.of(
						ReadinessDimensionType.FATIGUE,
						ReadinessDimensionType.MUSCLE_SORENESS,
						ReadinessDimensionType.STRESS));
		assertThat(adjustments).extracting(TrainingRecommendationAdjustment::type)
				.containsExactly(
						TrainingAdjustmentType.REDUCE_INTENSITY,
						TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
						TrainingAdjustmentType.REDUCE_SESSION_DURATION,
						TrainingAdjustmentType.INCREASE_REST,
						TrainingAdjustmentType.PREFER_LOWER_IMPACT_VARIATIONS);
	}

	@Test
	void moodLimitingAddsNoPhysicalAdjustment() {
		List<TrainingRecommendationAdjustment> adjustments = TrainingAdjustmentResolver.resolve(
				TrainingRecommendationAction.MODIFY_SESSION,
				ReadinessBand.MODERATE,
				List.of(ReadinessDimensionType.MOOD));
		assertThat(adjustments).extracting(TrainingRecommendationAdjustment::type)
				.containsExactly(TrainingAdjustmentType.REDUCE_TOTAL_VOLUME);
	}

	private static DailyAthleteStateScheduledOccurrenceSnapshot scheduledOccurrence() {
		return occurrence(WorkoutOccurrenceStatus.SCHEDULED);
	}

	private static DailyAthleteStateScheduledOccurrenceSnapshot completedOccurrence() {
		return occurrence(WorkoutOccurrenceStatus.COMPLETED);
	}

	private static DailyAthleteStateScheduledOccurrenceSnapshot occurrence(WorkoutOccurrenceStatus status) {
		return new DailyAthleteStateScheduledOccurrenceSnapshot(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				LocalDate.of(2026, 7, 31),
				status,
				"Home Gym",
				null,
				0);
	}

	private static DailyReadinessAssessment assessment(
			ReadinessBand band,
			ReadinessScore score,
			List<ReadinessDimensionType> limiting) {
		return DailyReadinessAssessment.rehydrate(
				DailyReadinessAssessmentId.generate(),
				ATHLETE_ID,
				LocalDate.of(2026, 7, 31),
				SNAPSHOT_ID,
				3,
				ReadinessAlgorithmVersion.READINESS_V1,
				score,
				band,
				band == ReadinessBand.INSUFFICIENT_DATA
						? ReadinessDataSufficiency.INSUFFICIENT
						: ReadinessDataSufficiency.SUFFICIENT,
				band == ReadinessBand.INSUFFICIENT_DATA
						? ReadinessReasonCode.READINESS_DATA_INSUFFICIENT
						: ReadinessReasonCode.READINESS_CALCULATED,
				limiting.size(),
				5,
				Instant.now(CLOCK),
				Instant.now(CLOCK),
				List.of(),
				limiting,
				List.of());
	}

	private static DailyAthleteStateSnapshot snapshot(
			List<DailyAthleteStateScheduledOccurrenceSnapshot> occurrences,
			long scheduled,
			long completed,
			long skipped,
			long cancelled,
			long inProgress) {
		return DailyAthleteStateSnapshot.rehydrate(
				SNAPSHOT_ID,
				ATHLETE_ID,
				LocalDate.of(2026, 7, 31),
				3,
				true,
				"fingerprint",
				DailyAthleteStateGenerationReason.MANUAL,
				Instant.now(CLOCK),
				DailyAthleteStateCompleteness.COMPLETE,
				7,
				RecoveryAnalyticsCalculationVersion.RECOVERY_ANALYTICS_V1,
				true,
				UUID.randomUUID(),
				1L,
				360,
				2,
				5,
				4,
				4,
				2,
				2,
				Instant.now(CLOCK),
				Instant.now(CLOCK),
				scheduled,
				completed,
				0,
				0,
				0,
				0,
				0,
				BigDecimal.ZERO,
				0,
				BigDecimal.ZERO,
				null,
				null,
				0,
				0,
				0,
				0,
				0,
				scheduled,
				scheduled,
				completed,
				skipped,
				cancelled,
				inProgress,
				Instant.now(CLOCK),
				List.of(),
				List.of(),
				List.of(),
				List.of(),
				occurrences);
	}

}
