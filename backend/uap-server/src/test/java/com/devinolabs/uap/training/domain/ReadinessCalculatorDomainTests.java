package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ReadinessCalculatorDomainTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void burdenAndSupportiveZScoreNormalization() {
		assertThat(ReadinessDimensionNormalizer.fromZScore(new BigDecimal("1.5"), true))
				.isEqualByComparingTo("20.00");
		assertThat(ReadinessDimensionNormalizer.fromZScore(new BigDecimal("-1.5"), true))
				.isEqualByComparingTo("100.00");
		assertThat(ReadinessDimensionNormalizer.fromZScore(new BigDecimal("1.5"), false))
				.isEqualByComparingTo("100.00");
		assertThat(ReadinessDimensionNormalizer.fromZScore(new BigDecimal("-1.5"), false))
				.isEqualByComparingTo("20.00");
		assertThat(ReadinessDimensionNormalizer.fromZScore(new BigDecimal("0.0"), true))
				.isEqualByComparingTo("70.00");
	}

	@Test
	void sleepDurationUsesMagnitudeNotDirection() {
		assertThat(ReadinessDimensionNormalizer.fromComparisonBand(
				RecoveryComparisonBand.ABOVE_BASELINE, true)).isEqualByComparingTo("45.00");
		DailyAthleteStateRecoveryMetricSnapshot sleep = metric(
				RecoveryMetricType.SLEEP_DURATION,
				new BigDecimal("360"),
				new BigDecimal("420"),
				null,
				RecoveryComparisonBand.BELOW_BASELINE,
				RecoveryBaselineDataSufficiency.SUFFICIENT);
		var normalized = ReadinessDimensionNormalizer.normalize(ReadinessDimensionType.SLEEP_DURATION, sleep).orElseThrow();
		assertThat(normalized.normalizedScore()).isEqualByComparingTo("65.00");
	}

	@Test
	void readinessScoreBandBoundaries() {
		assertThat(ReadinessScore.of(new BigDecimal("49.99")).band()).isEqualTo(ReadinessBand.LOW);
		assertThat(ReadinessScore.of(new BigDecimal("50.00")).band()).isEqualTo(ReadinessBand.MODERATE);
		assertThat(ReadinessScore.of(new BigDecimal("74.99")).band()).isEqualTo(ReadinessBand.MODERATE);
		assertThat(ReadinessScore.of(new BigDecimal("75.00")).band()).isEqualTo(ReadinessBand.HIGH);
	}

	@Test
	void insufficientCoreDimensionsYieldNullScore() {
		DailyAthleteStateSnapshot snapshot = snapshotWithMetrics(List.of(
				metric(RecoveryMetricType.FATIGUE, "5", "3", "1.8", RecoveryComparisonBand.FAR_ABOVE_BASELINE),
				metric(RecoveryMetricType.MOOD, "2", "4", "-1.2", RecoveryComparisonBand.BELOW_BASELINE)));
		ReadinessCalculator.CalculationResult result = ReadinessCalculator.calculate(snapshot, CLOCK);
		assertThat(result.readinessScore()).isNull();
		assertThat(result.readinessBand()).isEqualTo(ReadinessBand.INSUFFICIENT_DATA);
		assertThat(result.summaryReasonCode()).isEqualTo(ReadinessReasonCode.READINESS_DATA_INSUFFICIENT);
	}

	@Test
	void criticalBandScenarioProducesDeterministicLowScore() {
		DailyAthleteStateSnapshot snapshot = snapshotWithMetrics(List.of(
				metric(RecoveryMetricType.FATIGUE, "5", "3.14", "2.0", RecoveryComparisonBand.FAR_ABOVE_BASELINE),
				metric(RecoveryMetricType.MUSCLE_SORENESS, "4", "2.5", "0.8", RecoveryComparisonBand.ABOVE_BASELINE),
				metric(RecoveryMetricType.STRESS, "4", "2.7", "0.9", RecoveryComparisonBand.ABOVE_BASELINE),
				metric(RecoveryMetricType.MOOD, "2", "3.7", "-1.0", RecoveryComparisonBand.BELOW_BASELINE),
				metric(RecoveryMetricType.MOTIVATION, "2", "3.6", "-1.0", RecoveryComparisonBand.BELOW_BASELINE),
				metric(RecoveryMetricType.SLEEP_QUALITY, "2", "3.4", "-1.0", RecoveryComparisonBand.BELOW_BASELINE),
				metric(RecoveryMetricType.SLEEP_DURATION, "360", "424", null, RecoveryComparisonBand.BELOW_BASELINE)));

		ReadinessCalculator.CalculationResult result = ReadinessCalculator.calculate(snapshot, CLOCK);
		assertThat(result.readinessScore()).isNotNull();
		assertThat(result.readinessScore().value()).isEqualByComparingTo("42.00");
		assertThat(result.readinessBand()).isEqualTo(ReadinessBand.LOW);
		assertThat(result.dataSufficiency()).isEqualTo(ReadinessDataSufficiency.SUFFICIENT);
		assertThat(result.limitingDimensions()).containsExactly(
				ReadinessDimensionType.FATIGUE,
				ReadinessDimensionType.MOOD,
				ReadinessDimensionType.MOTIVATION);
		assertThat(result.contributions().stream()
				.filter(c -> c.dimensionType() == ReadinessDimensionType.TRAINING_LOAD_CONTEXT)
				.findFirst().orElseThrow().reasonCode())
				.isEqualTo(ReadinessReasonCode.CONTEXT_ONLY);
		BigDecimal effectiveWeightSum = result.contributions().stream()
				.filter(c -> c.effectiveWeight() != null)
				.map(ReadinessDimensionContribution::effectiveWeight)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		assertThat(effectiveWeightSum).isEqualByComparingTo("1.00000");
	}

	@Test
	void missingSleepRenormalizesWeights() {
		DailyAthleteStateSnapshot snapshot = snapshotWithMetrics(List.of(
				metric(RecoveryMetricType.FATIGUE, "3", "3", "0", RecoveryComparisonBand.WITHIN_BASELINE_RANGE),
				metric(RecoveryMetricType.MUSCLE_SORENESS, "3", "3", "0", RecoveryComparisonBand.WITHIN_BASELINE_RANGE),
				metric(RecoveryMetricType.STRESS, "3", "3", "0", RecoveryComparisonBand.WITHIN_BASELINE_RANGE),
				metric(RecoveryMetricType.MOOD, "3", "3", "0", RecoveryComparisonBand.WITHIN_BASELINE_RANGE),
				metric(RecoveryMetricType.MOTIVATION, "3", "3", "0", RecoveryComparisonBand.WITHIN_BASELINE_RANGE)));
		// sleep missing entirely from metrics list → TARGET_VALUE_MISSING / DIMENSION_EXCLUDED style
		ReadinessCalculator.CalculationResult result = ReadinessCalculator.calculate(snapshot, CLOCK);
		assertThat(result.readinessScore()).isNotNull();
		assertThat(result.readinessScore().value()).isEqualByComparingTo("70.00");
		BigDecimal effectiveWeightSum = result.contributions().stream()
				.filter(c -> c.effectiveWeight() != null)
				.map(ReadinessDimensionContribution::effectiveWeight)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		assertThat(effectiveWeightSum).isEqualByComparingTo("1.00000");
	}

	private static DailyAthleteStateRecoveryMetricSnapshot metric(
			RecoveryMetricType type,
			String target,
			String mean,
			String z,
			RecoveryComparisonBand band) {
		return metric(
				type,
				new BigDecimal(target),
				new BigDecimal(mean),
				z == null ? null : new BigDecimal(z),
				band,
				RecoveryBaselineDataSufficiency.SUFFICIENT);
	}

	private static DailyAthleteStateRecoveryMetricSnapshot metric(
			RecoveryMetricType type,
			BigDecimal target,
			BigDecimal mean,
			BigDecimal z,
			RecoveryComparisonBand band,
			RecoveryBaselineDataSufficiency sufficiency) {
		return new DailyAthleteStateRecoveryMetricSnapshot(
				type,
				target,
				type.scaleDirection(),
				7,
				sufficiency,
				mean,
				mean,
				mean,
				mean,
				BigDecimal.ONE,
				target.subtract(mean),
				null,
				z,
				band,
				RecoveryAnalyticsReasonCode.BASELINE_AVAILABLE);
	}

	private static DailyAthleteStateSnapshot snapshotWithMetrics(
			List<DailyAthleteStateRecoveryMetricSnapshot> metrics) {
		List<DailyAthleteStateRecoveryMetricSnapshot> all = new ArrayList<>(metrics);
		for (RecoveryMetricType type : RecoveryMetricType.values()) {
			if (all.stream().noneMatch(m -> m.metricType() == type)) {
				all.add(new DailyAthleteStateRecoveryMetricSnapshot(
						type,
						null,
						type.scaleDirection(),
						0,
						RecoveryBaselineDataSufficiency.INSUFFICIENT,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						RecoveryComparisonBand.INSUFFICIENT_DATA,
						RecoveryAnalyticsReasonCode.TARGET_VALUE_MISSING));
			}
		}
		return DailyAthleteStateSnapshot.rehydrate(
				DailyAthleteStateSnapshotId.generate(),
				AthleteId.of(UUID.randomUUID()),
				LocalDate.of(2026, 7, 31),
				1,
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
				1,
				1,
				1,
				0,
				4,
				10,
				20,
				new BigDecimal("2400.000"),
				1920,
				new BigDecimal("5000.000"),
				new BigDecimal("552.50"),
				new BigDecimal("8.5"),
				65,
				1,
				1,
				1,
				1,
				1,
				1,
				1,
				0,
				0,
				0,
				Instant.now(CLOCK),
				all,
				List.of(),
				List.of(),
				List.of(),
				List.of());
	}

}
