package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Builds prior-only athlete-specific baselines. The target date is never included.
 */
public final class RecoveryBaselineCalculator {

	public static final int[] SUPPORTED_WINDOWS = { 7, 14, 28 };

	private RecoveryBaselineCalculator() {
	}

	public static void requireSupportedWindow(int windowDays) {
		for (int supported : SUPPORTED_WINDOWS) {
			if (supported == windowDays) {
				return;
			}
		}
		throw new InvalidRecoveryBaselineWindowException(
				"baselineWindowDays must be one of 7, 14, or 28");
	}

	public static RecoveryMetricBaseline calculate(
			RecoveryMetricType metricType,
			LocalDate targetDate,
			int windowDays,
			List<DailyRecoveryCheckIn> checkInsBeforeOrOnRange,
			Clock clock) {
		Objects.requireNonNull(metricType, "metricType must not be null");
		Objects.requireNonNull(targetDate, "targetDate must not be null");
		requireSupportedWindow(windowDays);
		Objects.requireNonNull(checkInsBeforeOrOnRange, "checkInsBeforeOrOnRange must not be null");
		Objects.requireNonNull(clock, "clock must not be null");
		LocalDate windowStart = targetDate.minusDays(windowDays);
		LocalDate windowEnd = targetDate.minusDays(1);
		List<DailyRecoveryCheckIn> eligible = checkInsBeforeOrOnRange.stream()
				.filter(checkIn -> !checkIn.checkInDate().isBefore(windowStart))
				.filter(checkIn -> !checkIn.checkInDate().isAfter(windowEnd))
				.filter(checkIn -> checkIn.checkInDate().isBefore(targetDate))
				.sorted(Comparator.comparing(DailyRecoveryCheckIn::checkInDate)
						.thenComparing(checkIn -> checkIn.id().value()))
				.toList();
		List<BigDecimal> values = new ArrayList<>();
		LocalDate first = null;
		LocalDate last = null;
		for (DailyRecoveryCheckIn checkIn : eligible) {
			Optional<BigDecimal> value = extract(metricType, checkIn);
			if (value.isEmpty()) {
				continue;
			}
			values.add(value.get());
			if (first == null) {
				first = checkIn.checkInDate();
			}
			last = checkIn.checkInDate();
		}
		var stats = RecoveryMetricStatisticsCalculator.calculate(values);
		return new RecoveryMetricBaseline(
				metricType,
				windowDays,
				windowStart,
				windowEnd,
				stats.observationCount(),
				RecoveryBaselineDataSufficiency.of(stats.observationCount()),
				stats.mean(),
				stats.median(),
				stats.minimum(),
				stats.maximum(),
				stats.standardDeviation(),
				first,
				last,
				Instant.now(clock));
	}

	public static Optional<BigDecimal> extract(RecoveryMetricType metricType, DailyRecoveryCheckIn checkIn) {
		Objects.requireNonNull(metricType, "metricType must not be null");
		Objects.requireNonNull(checkIn, "checkIn must not be null");
		return switch (metricType) {
			case SLEEP_DURATION -> checkIn.sleepDurationMinutes() == null
					? Optional.empty()
					: Optional.of(BigDecimal.valueOf(checkIn.sleepDurationMinutes()));
			case SLEEP_QUALITY -> checkIn.sleepQuality() == null
					? Optional.empty()
					: Optional.of(BigDecimal.valueOf(checkIn.sleepQuality().value()));
			case FATIGUE -> Optional.of(BigDecimal.valueOf(checkIn.fatigue().value()));
			case MUSCLE_SORENESS -> Optional.of(BigDecimal.valueOf(checkIn.muscleSoreness().value()));
			case STRESS -> Optional.of(BigDecimal.valueOf(checkIn.stress().value()));
			case MOOD -> Optional.of(BigDecimal.valueOf(checkIn.mood().value()));
			case MOTIVATION -> Optional.of(BigDecimal.valueOf(checkIn.motivation().value()));
		};
	}

}
