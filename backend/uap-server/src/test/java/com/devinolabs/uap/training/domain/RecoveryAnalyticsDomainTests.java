package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RecoveryAnalyticsDomainTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void statisticsCalculateMeanMedianAndSampleStandardDeviation() {
		var stats = RecoveryMetricStatisticsCalculator.calculate(List.of(
				new BigDecimal("2"),
				new BigDecimal("3"),
				new BigDecimal("4"),
				new BigDecimal("5")));
		assertThat(stats.observationCount()).isEqualTo(4);
		assertThat(stats.mean()).isEqualByComparingTo("3.50");
		assertThat(stats.median()).isEqualByComparingTo("3.50");
		assertThat(stats.minimum()).isEqualByComparingTo("2");
		assertThat(stats.maximum()).isEqualByComparingTo("5");
		assertThat(stats.standardDeviation()).isEqualByComparingTo("1.2910");
	}

	@Test
	void medianUsesDecimalAverageForEvenCounts() {
		var stats = RecoveryMetricStatisticsCalculator.calculate(List.of(
				new BigDecimal("2"),
				new BigDecimal("3"),
				new BigDecimal("4"),
				new BigDecimal("5")));
		assertThat(stats.median()).isEqualByComparingTo("3.50");
	}

	@Test
	void standardDeviationIsNullForSingleObservation() {
		var stats = RecoveryMetricStatisticsCalculator.calculate(List.of(new BigDecimal("4")));
		assertThat(stats.standardDeviation()).isNull();
	}

	@Test
	void deviationBandsUseStandardizedThresholds() {
		assertThat(RecoveryMetricDeviationCalculator.band(
				new BigDecimal("-1.50"), new BigDecimal("2"), new BigDecimal("4"),
				RecoveryBaselineDataSufficiency.SUFFICIENT))
				.isEqualTo(RecoveryComparisonBand.FAR_BELOW_BASELINE);
		assertThat(RecoveryMetricDeviationCalculator.band(
				new BigDecimal("-0.50"), new BigDecimal("3"), new BigDecimal("4"),
				RecoveryBaselineDataSufficiency.SUFFICIENT))
				.isEqualTo(RecoveryComparisonBand.BELOW_BASELINE);
		assertThat(RecoveryMetricDeviationCalculator.band(
				new BigDecimal("0.49"), new BigDecimal("4"), new BigDecimal("4"),
				RecoveryBaselineDataSufficiency.SUFFICIENT))
				.isEqualTo(RecoveryComparisonBand.WITHIN_BASELINE_RANGE);
		assertThat(RecoveryMetricDeviationCalculator.band(
				new BigDecimal("1.49"), new BigDecimal("5"), new BigDecimal("4"),
				RecoveryBaselineDataSufficiency.SUFFICIENT))
				.isEqualTo(RecoveryComparisonBand.ABOVE_BASELINE);
		assertThat(RecoveryMetricDeviationCalculator.band(
				new BigDecimal("1.50"), new BigDecimal("6"), new BigDecimal("4"),
				RecoveryBaselineDataSufficiency.SUFFICIENT))
				.isEqualTo(RecoveryComparisonBand.FAR_ABOVE_BASELINE);
	}

	@Test
	void zeroVarianceEqualTargetProducesZeroStandardizedDeviation() {
		RecoveryMetricBaseline baseline = baselineWithStats(
				RecoveryMetricType.FATIGUE,
				3,
				new BigDecimal("3"),
				BigDecimal.ZERO);
		var result = RecoveryMetricDeviationCalculator.compare(
				RecoveryMetricType.FATIGUE, new BigDecimal("3"), baseline);
		assertThat(result.standardizedDeviation()).isEqualByComparingTo("0.0000");
		assertThat(result.reasonCode()).isEqualTo(RecoveryAnalyticsReasonCode.BASELINE_AVAILABLE);
	}

	@Test
	void zeroVarianceUnequalTargetReturnsZeroBaselineVarianceReason() {
		RecoveryMetricBaseline baseline = baselineWithStats(
				RecoveryMetricType.FATIGUE,
				3,
				new BigDecimal("3"),
				BigDecimal.ZERO);
		var result = RecoveryMetricDeviationCalculator.compare(
				RecoveryMetricType.FATIGUE, new BigDecimal("4"), baseline);
		assertThat(result.standardizedDeviation()).isNull();
		assertThat(result.reasonCode()).isEqualTo(RecoveryAnalyticsReasonCode.ZERO_BASELINE_VARIANCE);
	}

	@Test
	void ordinalRatingsOmitPercentageDifference() {
		RecoveryMetricBaseline baseline = baselineWithStats(
				RecoveryMetricType.FATIGUE,
				7,
				new BigDecimal("3"),
				new BigDecimal("0.8165"));
		var result = RecoveryMetricDeviationCalculator.compare(
				RecoveryMetricType.FATIGUE, new BigDecimal("5"), baseline);
		assertThat(result.percentageDifference()).isNull();
		assertThat(result.absoluteDifference()).isEqualByComparingTo("2.00");
	}

	@Test
	void sleepDurationIncludesPercentageDifferenceWhenMeanPositive() {
		RecoveryMetricBaseline baseline = baselineWithStats(
				RecoveryMetricType.SLEEP_DURATION,
				7,
				new BigDecimal("420"),
				new BigDecimal("15"));
		var result = RecoveryMetricDeviationCalculator.compare(
				RecoveryMetricType.SLEEP_DURATION, new BigDecimal("360"), baseline);
		assertThat(result.percentageDifference()).isEqualByComparingTo("-14.29");
	}

	@Test
	void trendDetectsIncreasingDecreasingAndStableDirections() {
		assertThat(RecoveryTrendCalculator.calculate(
				RecoveryMetricType.FATIGUE,
				List.of(
						new BigDecimal("2"), new BigDecimal("2"),
						new BigDecimal("4"), new BigDecimal("4"))))
				.isEqualTo(RecoveryTrendDirection.INCREASING);
		assertThat(RecoveryTrendCalculator.calculate(
				RecoveryMetricType.FATIGUE,
				List.of(
						new BigDecimal("4"), new BigDecimal("4"),
						new BigDecimal("2"), new BigDecimal("2"))))
				.isEqualTo(RecoveryTrendDirection.DECREASING);
		assertThat(RecoveryTrendCalculator.calculate(
				RecoveryMetricType.FATIGUE,
				List.of(
						new BigDecimal("3"), new BigDecimal("3"),
						new BigDecimal("3"), new BigDecimal("3"))))
				.isEqualTo(RecoveryTrendDirection.STABLE);
	}

	@Test
	void trendUsesFifteenMinuteThresholdForSleepDuration() {
		assertThat(RecoveryTrendCalculator.calculate(
				RecoveryMetricType.SLEEP_DURATION,
				List.of(
						new BigDecimal("400"), new BigDecimal("400"),
						new BigDecimal("430"), new BigDecimal("430"))))
				.isEqualTo(RecoveryTrendDirection.INCREASING);
	}

	@Test
	void rollingAveragesReturnNullUntilWindowFilled() {
		List<BigDecimal> rolling = RecoveryRollingAverageCalculator.rollingAverages(
				List.of(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"), new BigDecimal("4")),
				3);
		assertThat(rolling).containsExactly(null, null, new BigDecimal("2.00"), new BigDecimal("3.00"));
	}

	@Test
	void baselineExcludesTargetDate() {
		LocalDate targetDate = LocalDate.of(2026, 7, 31);
		List<DailyRecoveryCheckIn> checkIns = List.of(
				checkIn(LocalDate.of(2026, 7, 30), 3),
				checkIn(targetDate, 5));
		RecoveryMetricBaseline baseline = RecoveryBaselineCalculator.calculate(
				RecoveryMetricType.FATIGUE,
				targetDate,
				7,
				checkIns,
				CLOCK);
		assertThat(baseline.observationCount()).isEqualTo(1);
		assertThat(baseline.mean()).isEqualByComparingTo("3.00");
	}

	@Test
	void unsupportedBaselineWindowIsRejected() {
		assertThatThrownBy(() -> RecoveryBaselineCalculator.requireSupportedWindow(10))
				.isInstanceOf(InvalidRecoveryBaselineWindowException.class);
	}

	@Test
	void dataSufficiencyLabelsFollowObservationCountRules() {
		assertThat(RecoveryBaselineDataSufficiency.of(0)).isEqualTo(RecoveryBaselineDataSufficiency.INSUFFICIENT);
		assertThat(RecoveryBaselineDataSufficiency.of(2)).isEqualTo(RecoveryBaselineDataSufficiency.INSUFFICIENT);
		assertThat(RecoveryBaselineDataSufficiency.of(3)).isEqualTo(RecoveryBaselineDataSufficiency.LIMITED);
		assertThat(RecoveryBaselineDataSufficiency.of(6)).isEqualTo(RecoveryBaselineDataSufficiency.LIMITED);
		assertThat(RecoveryBaselineDataSufficiency.of(7)).isEqualTo(RecoveryBaselineDataSufficiency.SUFFICIENT);
	}

	private static RecoveryMetricBaseline baselineWithStats(
			RecoveryMetricType metricType,
			int observationCount,
			BigDecimal mean,
			BigDecimal stdDev) {
		return new RecoveryMetricBaseline(
				metricType,
				7,
				LocalDate.of(2026, 7, 24),
				LocalDate.of(2026, 7, 30),
				observationCount,
				RecoveryBaselineDataSufficiency.of(observationCount),
				mean,
				mean,
				mean,
				mean,
				stdDev,
				LocalDate.of(2026, 7, 24),
				LocalDate.of(2026, 7, 30),
				Instant.now(CLOCK));
	}

	private static DailyRecoveryCheckIn checkIn(LocalDate date, int fatigue) {
		return DailyRecoveryCheckIn.rehydrate(
				DailyRecoveryCheckInId.generate(),
				AthleteId.of(UUID.randomUUID()),
				date,
				null,
				null,
				FatigueRating.of(fatigue),
				MuscleSorenessRating.of(2),
				StressRating.of(2),
				MoodRating.of(4),
				TrainingMotivationRating.of(4),
				RecoveryCheckInCompleteness.PARTIAL,
				List.of(),
				null,
				RecoveryCheckInSource.ATHLETE_REPORTED,
				Instant.now(CLOCK),
				Instant.now(CLOCK),
				Instant.now(CLOCK),
				0L);
	}

}
