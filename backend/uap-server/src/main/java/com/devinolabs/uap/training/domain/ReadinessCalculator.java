package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic READINESS_V1 calculator.
 * Consumes only immutable DailyAthleteStateSnapshot facts.
 */
public final class ReadinessCalculator {

	public static final ReadinessAlgorithmVersion ALGORITHM_VERSION = ReadinessAlgorithmVersion.READINESS_V1;
	public static final int MIN_CORE_USABLE_DIMENSIONS = 3;

	private static final MathContext MATH = MathContext.DECIMAL128;
	private static final Map<ReadinessDimensionType, BigDecimal> CONFIGURED_WEIGHTS = configuredWeights();

	private ReadinessCalculator() {
	}

	public static CalculationResult calculate(DailyAthleteStateSnapshot snapshot, Clock clock) {
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		Objects.requireNonNull(clock, "clock must not be null");

		Map<RecoveryMetricType, DailyAthleteStateRecoveryMetricSnapshot> metricsByType = indexMetrics(snapshot);
		List<DraftContribution> drafts = new ArrayList<>();

		for (ReadinessDimensionType dimensionType : ReadinessDimensionType.values()) {
			if (dimensionType == ReadinessDimensionType.TRAINING_LOAD_CONTEXT) {
				drafts.add(contextOnly(dimensionType));
				continue;
			}
			Optional<RecoveryMetricType> sourceMetric = dimensionType.sourceMetricType();
			DailyAthleteStateRecoveryMetricSnapshot metric = sourceMetric
					.map(metricsByType::get)
					.orElse(null);
			drafts.add(buildRecoveryDraft(dimensionType, metric));
		}

		long coreUsable = drafts.stream()
				.filter(d -> d.dimensionType().coreRecovery())
				.filter(DraftContribution::usable)
				.count();

		ReadinessDataSufficiency sufficiency = ReadinessDataSufficiencyResolver.resolve(coreUsable);
		Instant assessedAt = Instant.now(clock);

		if (coreUsable < MIN_CORE_USABLE_DIMENSIONS) {
			List<ReadinessDimensionContribution> contributions = drafts.stream()
					.map(d -> d.toContribution(null, null))
					.toList();
			return new CalculationResult(
					null,
					ReadinessBand.INSUFFICIENT_DATA,
					sufficiency,
					ReadinessReasonCode.READINESS_DATA_INSUFFICIENT,
					contributions,
					List.of(),
					List.of(),
					assessedAt);
		}

		BigDecimal availableWeightTotal = drafts.stream()
				.filter(DraftContribution::usable)
				.map(DraftContribution::configuredWeight)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		if (availableWeightTotal.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ReadinessNumericOverflowException("Available readiness weight total must be positive");
		}

		List<ReadinessDimensionContribution> weighted = new ArrayList<>();
		BigDecimal scoreAccumulator = BigDecimal.ZERO;
		for (DraftContribution draft : drafts) {
			if (!draft.usable()) {
				weighted.add(draft.toContribution(null, null));
				continue;
			}
			BigDecimal effectiveWeight = draft.configuredWeight()
					.divide(availableWeightTotal, 5, RoundingMode.HALF_UP);
			BigDecimal contribution = draft.normalizedScore()
					.multiply(effectiveWeight, MATH)
					.setScale(4, RoundingMode.HALF_UP);
			scoreAccumulator = scoreAccumulator.add(contribution, MATH);
			weighted.add(draft.toContribution(effectiveWeight, contribution));
		}

		ReadinessScore score;
		try {
			score = ReadinessScore.of(scoreAccumulator);
		}
		catch (ArithmeticException ex) {
			throw new ReadinessNumericOverflowException("Readiness score overflow", ex);
		}

		List<ReadinessDimensionType> limiting = ReadinessSummaryResolver.limiting(weighted, 3);
		List<ReadinessDimensionType> strongest = ReadinessSummaryResolver.strongest(weighted, 3);
		List<ReadinessDimensionContribution> ranked = ReadinessSummaryResolver.applyRanks(
				weighted, limiting, strongest);

		return new CalculationResult(
				score,
				score.band(),
				sufficiency,
				ReadinessReasonCode.READINESS_CALCULATED,
				ranked,
				limiting,
				strongest,
				assessedAt);
	}

	private static DraftContribution buildRecoveryDraft(
			ReadinessDimensionType dimensionType,
			DailyAthleteStateRecoveryMetricSnapshot metric) {
		BigDecimal configuredWeight = CONFIGURED_WEIGHTS.get(dimensionType);
		if (metric == null) {
			return DraftContribution.unavailable(
					dimensionType,
					dimensionType.sourceMetricType().orElse(null),
					configuredWeight,
					ReadinessReasonCode.DIMENSION_EXCLUDED);
		}
		if (metric.targetValue() == null) {
			return DraftContribution.unavailable(
					dimensionType,
					metric.metricType(),
					configuredWeight,
					ReadinessReasonCode.TARGET_VALUE_MISSING,
					metric);
		}
		if (metric.dataSufficiency() == RecoveryBaselineDataSufficiency.INSUFFICIENT
				|| metric.comparisonBand() == RecoveryComparisonBand.INSUFFICIENT_DATA
				|| metric.baselineMean() == null) {
			return DraftContribution.unavailable(
					dimensionType,
					metric.metricType(),
					configuredWeight,
					ReadinessReasonCode.BASELINE_INSUFFICIENT,
					metric);
		}
		Optional<ReadinessDimensionNormalizer.NormalizedDimension> normalized =
				ReadinessDimensionNormalizer.normalize(dimensionType, metric);
		if (normalized.isEmpty()) {
			return DraftContribution.unavailable(
					dimensionType,
					metric.metricType(),
					configuredWeight,
					ReadinessReasonCode.BASELINE_INSUFFICIENT,
					metric);
		}
		return DraftContribution.usable(
				dimensionType,
				metric.metricType(),
				configuredWeight,
				normalized.get().normalizedScore(),
				normalized.get().reasonCode(),
				metric);
	}

	private static DraftContribution contextOnly(ReadinessDimensionType dimensionType) {
		return new DraftContribution(
				dimensionType,
				null,
				false,
				null,
				null,
				null,
				null,
				null,
				null,
				CONFIGURED_WEIGHTS.get(dimensionType),
				ReadinessReasonCode.CONTEXT_ONLY,
				false);
	}

	private static Map<RecoveryMetricType, DailyAthleteStateRecoveryMetricSnapshot> indexMetrics(
			DailyAthleteStateSnapshot snapshot) {
		Map<RecoveryMetricType, DailyAthleteStateRecoveryMetricSnapshot> indexed =
				new EnumMap<>(RecoveryMetricType.class);
		for (DailyAthleteStateRecoveryMetricSnapshot metric : snapshot.recoveryMetrics()) {
			indexed.put(metric.metricType(), metric);
		}
		return indexed;
	}

	private static Map<ReadinessDimensionType, BigDecimal> configuredWeights() {
		Map<ReadinessDimensionType, BigDecimal> weights = new EnumMap<>(ReadinessDimensionType.class);
		weights.put(ReadinessDimensionType.FATIGUE, new BigDecimal("0.20000"));
		weights.put(ReadinessDimensionType.MUSCLE_SORENESS, new BigDecimal("0.15000"));
		weights.put(ReadinessDimensionType.STRESS, new BigDecimal("0.15000"));
		weights.put(ReadinessDimensionType.MOOD, new BigDecimal("0.15000"));
		weights.put(ReadinessDimensionType.MOTIVATION, new BigDecimal("0.15000"));
		weights.put(ReadinessDimensionType.SLEEP_QUALITY, new BigDecimal("0.10000"));
		weights.put(ReadinessDimensionType.SLEEP_DURATION, new BigDecimal("0.10000"));
		weights.put(ReadinessDimensionType.TRAINING_LOAD_CONTEXT, new BigDecimal("0.00000"));
		return Map.copyOf(weights);
	}

	private record DraftContribution(
			ReadinessDimensionType dimensionType,
			RecoveryMetricType sourceMetricType,
			boolean usable,
			RecoveryBaselineDataSufficiency baselineSufficiency,
			BigDecimal targetValue,
			BigDecimal baselineMean,
			BigDecimal standardizedDeviation,
			RecoveryComparisonBand comparisonBand,
			BigDecimal normalizedScore,
			BigDecimal configuredWeight,
			ReadinessReasonCode reasonCode,
			boolean availableFlag) {

		static DraftContribution usable(
				ReadinessDimensionType dimensionType,
				RecoveryMetricType sourceMetricType,
				BigDecimal configuredWeight,
				BigDecimal normalizedScore,
				ReadinessReasonCode reasonCode,
				DailyAthleteStateRecoveryMetricSnapshot metric) {
			return new DraftContribution(
					dimensionType,
					sourceMetricType,
					true,
					metric.dataSufficiency(),
					metric.targetValue(),
					metric.baselineMean(),
					metric.standardizedDeviation(),
					metric.comparisonBand(),
					normalizedScore,
					configuredWeight,
					reasonCode,
					true);
		}

		static DraftContribution unavailable(
				ReadinessDimensionType dimensionType,
				RecoveryMetricType sourceMetricType,
				BigDecimal configuredWeight,
				ReadinessReasonCode reasonCode) {
			return new DraftContribution(
					dimensionType,
					sourceMetricType,
					false,
					null,
					null,
					null,
					null,
					null,
					null,
					configuredWeight,
					reasonCode,
					false);
		}

		static DraftContribution unavailable(
				ReadinessDimensionType dimensionType,
				RecoveryMetricType sourceMetricType,
				BigDecimal configuredWeight,
				ReadinessReasonCode reasonCode,
				DailyAthleteStateRecoveryMetricSnapshot metric) {
			return new DraftContribution(
					dimensionType,
					sourceMetricType,
					false,
					metric.dataSufficiency(),
					metric.targetValue(),
					metric.baselineMean(),
					metric.standardizedDeviation(),
					metric.comparisonBand(),
					null,
					configuredWeight,
					reasonCode,
					false);
		}

		ReadinessDimensionContribution toContribution(
				BigDecimal effectiveWeight,
				BigDecimal weightedContribution) {
			return new ReadinessDimensionContribution(
					dimensionType,
					sourceMetricType,
					availableFlag,
					baselineSufficiency,
					targetValue,
					baselineMean,
					standardizedDeviation,
					comparisonBand,
					normalizedScore,
					configuredWeight,
					effectiveWeight,
					weightedContribution,
					reasonCode,
					null,
					null);
		}
	}

	public record CalculationResult(
			ReadinessScore readinessScore,
			ReadinessBand readinessBand,
			ReadinessDataSufficiency dataSufficiency,
			ReadinessReasonCode summaryReasonCode,
			List<ReadinessDimensionContribution> contributions,
			List<ReadinessDimensionType> limitingDimensions,
			List<ReadinessDimensionType> strongestDimensions,
			Instant assessedAt) {
	}

}
