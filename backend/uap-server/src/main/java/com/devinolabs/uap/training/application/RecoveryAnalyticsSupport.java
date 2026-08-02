package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.BodySide;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.InvalidRecoveryBaselineWindowException;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsReasonCode;
import com.devinolabs.uap.training.domain.RecoveryBaselineCalculator;
import com.devinolabs.uap.training.domain.RecoveryMetricBaseline;
import com.devinolabs.uap.training.domain.RecoveryMetricDeviationCalculator;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.RecoveryRollingAverageCalculator;
import com.devinolabs.uap.training.domain.RecoveryTrendCalculator;
import com.devinolabs.uap.training.domain.RecoveryTrendDirection;

final class RecoveryAnalyticsSupport {

	private static final MathContext MATH = MathContext.DECIMAL128;
	private static final RecoveryMetricType[] ALL_METRICS = RecoveryMetricType.values();

	private RecoveryAnalyticsSupport() {
	}

	static void requireBaselineWindow(int baselineWindowDays) {
		try {
			RecoveryBaselineCalculator.requireSupportedWindow(baselineWindowDays);
		}
		catch (InvalidRecoveryBaselineWindowException ex) {
			throw ex;
		}
	}

	static void requireTrendDateRange(LocalDate startDate, LocalDate endDate) {
		Objects.requireNonNull(startDate, "startDate must not be null");
		Objects.requireNonNull(endDate, "endDate must not be null");
		if (endDate.isBefore(startDate)) {
			throw new InvalidRecoveryTrendDateRangeException("endDate must not be before startDate");
		}
		long span = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
		if (span > RecoveryCheckInSupport.MAX_LIST_RANGE_DAYS) {
			throw new InvalidRecoveryTrendDateRangeException(
					"Date range must not exceed " + RecoveryCheckInSupport.MAX_LIST_RANGE_DAYS + " days");
		}
	}

	static void requireTargetDate(LocalDate targetDate, Clock clock) {
		Objects.requireNonNull(targetDate, "targetDate must not be null");
		LocalDate today = LocalDate.now(clock);
		if (targetDate.isAfter(today)) {
			throw new RecoveryAnalyticsDateOutOfRangeException("targetDate must not be in the future");
		}
	}

	static RecoveryMetricType parseMetricType(String metricType) {
		Objects.requireNonNull(metricType, "metricType must not be null");
		try {
			return RecoveryMetricType.valueOf(metricType.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRecoveryMetricTypeException("Unsupported recovery metric type: " + metricType);
		}
	}

	static List<DailyRecoveryCheckIn> loadPriorCheckIns(
			DailyRecoveryCheckInRepository checkInRepository,
			AthleteId athleteId,
			LocalDate targetDate,
			int baselineWindowDays) {
		LocalDate windowStart = targetDate.minusDays(baselineWindowDays);
		LocalDate windowEnd = targetDate.minusDays(1);
		return checkInRepository.findAllByAthleteAndDateRange(athleteId, windowStart, windowEnd);
	}

	static List<DailyRecoveryCheckIn> loadCheckInsInRange(
			DailyRecoveryCheckInRepository checkInRepository,
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate) {
		return checkInRepository.findAllByAthleteAndDateRange(athleteId, startDate, endDate).stream()
				.sorted(Comparator.comparing(DailyRecoveryCheckIn::checkInDate)
						.thenComparing(checkIn -> checkIn.id().value()))
				.toList();
	}

	static Map<LocalDate, DailyTrainingLoadSummary> loadTrainingLoadByDate(
			TrainingLoadQueryRepository trainingLoadQueryRepository,
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate) {
		return RecoveryCheckInSupport.indexLoadByDate(trainingLoadQueryRepository.aggregateDaily(
				athleteId, startDate, endDate, null, null, null, null));
	}

	static List<RecoveryMetricBaselineResult> buildBaselines(
			LocalDate targetDate,
			int baselineWindowDays,
			List<DailyRecoveryCheckIn> priorCheckIns,
			Clock clock) {
		List<RecoveryMetricBaselineResult> baselines = new ArrayList<>();
		for (RecoveryMetricType metricType : ALL_METRICS) {
			RecoveryMetricBaseline baseline = RecoveryBaselineCalculator.calculate(
					metricType, targetDate, baselineWindowDays, priorCheckIns, clock);
			baselines.add(RecoveryMetricBaselineResult.from(baseline));
		}
		return baselines;
	}

	static List<RecoveryMetricDeviationResult> buildDeviations(
			DailyRecoveryCheckIn targetCheckIn,
			LocalDate targetDate,
			int baselineWindowDays,
			List<DailyRecoveryCheckIn> priorCheckIns,
			Clock clock) {
		List<RecoveryMetricDeviationResult> deviations = new ArrayList<>();
		for (RecoveryMetricType metricType : ALL_METRICS) {
			RecoveryMetricBaseline baseline = RecoveryBaselineCalculator.calculate(
					metricType, targetDate, baselineWindowDays, priorCheckIns, clock);
			Optional<BigDecimal> targetValue = RecoveryBaselineCalculator.extract(metricType, targetCheckIn);
			RecoveryMetricDeviationCalculator.RecoveryMetricDeviationResult deviation =
					RecoveryMetricDeviationCalculator.compare(
							metricType,
							targetValue.orElse(null),
							baseline);
			deviations.add(RecoveryMetricDeviationResult.from(
					deviation,
					RecoveryMetricBaselineResult.from(baseline)));
		}
		return deviations;
	}

	static List<RecoveryMetricDeviationResult> unavailableMetricComparisons(
			LocalDate targetDate,
			int baselineWindowDays,
			List<DailyRecoveryCheckIn> priorCheckIns,
			Clock clock) {
		List<RecoveryMetricDeviationResult> deviations = new ArrayList<>();
		for (RecoveryMetricType metricType : ALL_METRICS) {
			RecoveryMetricBaseline baseline = RecoveryBaselineCalculator.calculate(
					metricType, targetDate, baselineWindowDays, priorCheckIns, clock);
			RecoveryMetricDeviationCalculator.RecoveryMetricDeviationResult deviation =
					RecoveryMetricDeviationCalculator.RecoveryMetricDeviationResult.missingTarget(
							metricType, baseline);
			deviations.add(RecoveryMetricDeviationResult.from(
					deviation,
					RecoveryMetricBaselineResult.from(baseline)));
		}
		return deviations;
	}

	static RecoveryMetricTrendResult buildMetricTrend(
			RecoveryMetricType metricType,
			LocalDate startDate,
			LocalDate endDate,
			List<DailyRecoveryCheckIn> checkIns,
			Map<LocalDate, DailyTrainingLoadSummary> loadByDate,
			boolean includeTrainingLoad) {
		List<ObservedValue> observations = new ArrayList<>();
		for (DailyRecoveryCheckIn checkIn : checkIns) {
			Optional<BigDecimal> value = RecoveryBaselineCalculator.extract(metricType, checkIn);
			if (value.isEmpty()) {
				continue;
			}
			observations.add(new ObservedValue(
					checkIn.checkInDate(),
					checkIn.id().value(),
					value.get()));
		}
		List<BigDecimal> chronologicalValues = observations.stream()
				.map(ObservedValue::value)
				.toList();
		List<BigDecimal> rolling3 = RecoveryRollingAverageCalculator.rollingAverages(chronologicalValues, 3);
		List<BigDecimal> rolling7 = RecoveryRollingAverageCalculator.rollingAverages(chronologicalValues, 7);
		RecoveryTrendDirection trendDirection = RecoveryTrendCalculator.calculate(metricType, chronologicalValues);
		RecoveryAnalyticsReasonCode trendReason = trendDirection == RecoveryTrendDirection.INSUFFICIENT_DATA
				? RecoveryAnalyticsReasonCode.TREND_WINDOW_INSUFFICIENT
				: RecoveryAnalyticsReasonCode.BASELINE_AVAILABLE;
		List<RecoveryMetricTrendPointResult> points = new ArrayList<>();
		for (int index = 0; index < observations.size(); index++) {
			ObservedValue observation = observations.get(index);
			RecoveryTrainingLoadContextResult load = includeTrainingLoad
					? RecoveryCheckInSupport.loadContextForDate(observation.date(), loadByDate)
					: null;
			points.add(new RecoveryMetricTrendPointResult(
					observation.date(),
					observation.checkInId(),
					observation.value(),
					rolling3.get(index),
					rolling7.get(index),
					load));
		}
		return new RecoveryMetricTrendResult(
				metricType,
				startDate,
				endDate,
				observations.size(),
				trendDirection,
				trendReason,
				points);
	}

	static List<RecoveryMetricDashboardTrendResult> buildDashboardTrends(
			LocalDate targetDate,
			int baselineWindowDays,
			List<DailyRecoveryCheckIn> trendCheckIns) {
		List<RecoveryMetricDashboardTrendResult> trends = new ArrayList<>();
		for (RecoveryMetricType metricType : ALL_METRICS) {
			List<BigDecimal> values = new ArrayList<>();
			for (DailyRecoveryCheckIn checkIn : trendCheckIns) {
				RecoveryBaselineCalculator.extract(metricType, checkIn).ifPresent(values::add);
			}
			RecoveryTrendDirection direction = RecoveryTrendCalculator.calculate(metricType, values);
			trends.add(new RecoveryMetricDashboardTrendResult(metricType, direction, values.size()));
		}
		return trends;
	}

	static BodyAreaDiscomfortHistoryResult buildDiscomfortHistory(
			LocalDate startDate,
			LocalDate endDate,
			List<DailyRecoveryCheckIn> checkIns,
			BodyArea bodyAreaFilter,
			BodySide bodySideFilter) {
		List<BodyAreaDiscomfortHistoryEntryResult> entries = new ArrayList<>();
		Set<LocalDate> datesObserved = new HashSet<>();
		BigDecimal intensitySum = BigDecimal.ZERO;
		int intensityCount = 0;
		Integer maximumIntensity = null;
		LocalDate latestObservationDate = null;
		for (DailyRecoveryCheckIn checkIn : checkIns) {
			for (BodyAreaDiscomfortObservation observation : checkIn.discomfortAreas()) {
				if (bodyAreaFilter != null && observation.bodyArea() != bodyAreaFilter) {
					continue;
				}
				if (bodySideFilter != null && observation.side() != bodySideFilter) {
					continue;
				}
				entries.add(new BodyAreaDiscomfortHistoryEntryResult(
						checkIn.checkInDate(),
						checkIn.id().value(),
						observation.bodyArea(),
						observation.side(),
						observation.intensity(),
						observation.notes(),
						checkIn.version()));
				datesObserved.add(checkIn.checkInDate());
				intensitySum = intensitySum.add(BigDecimal.valueOf(observation.intensity().value()), MATH);
				intensityCount++;
				int intensityValue = observation.intensity().value();
				if (maximumIntensity == null || intensityValue > maximumIntensity) {
					maximumIntensity = intensityValue;
				}
				if (latestObservationDate == null || checkIn.checkInDate().isAfter(latestObservationDate)) {
					latestObservationDate = checkIn.checkInDate();
				}
			}
		}
		entries.sort(Comparator
				.comparing(BodyAreaDiscomfortHistoryEntryResult::date)
				.thenComparing(BodyAreaDiscomfortHistoryEntryResult::bodyArea)
				.thenComparing(BodyAreaDiscomfortHistoryEntryResult::side));
		BigDecimal averageIntensity = intensityCount == 0
				? null
				: intensitySum.divide(BigDecimal.valueOf(intensityCount), 2, RoundingMode.HALF_UP);
		return new BodyAreaDiscomfortHistoryResult(
				startDate,
				endDate,
				entries.size(),
				datesObserved.size(),
				averageIntensity,
				maximumIntensity,
				latestObservationDate,
				entries);
	}

	static Instant calculatedAt(Clock clock) {
		return Instant.now(clock);
	}

	private record ObservedValue(LocalDate date, UUID checkInId, BigDecimal value) {
	}

}
