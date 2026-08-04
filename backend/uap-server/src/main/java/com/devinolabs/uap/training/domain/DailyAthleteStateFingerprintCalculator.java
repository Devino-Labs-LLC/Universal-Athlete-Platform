package com.devinolabs.uap.training.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic SHA-256 fingerprint over canonical factual source state.
 * Excludes generation timestamps and snapshot identity.
 */
public final class DailyAthleteStateFingerprintCalculator {

	private DailyAthleteStateFingerprintCalculator() {
	}

	public static String calculate(DailyAthleteStateFingerprintInput input) {
		Objects.requireNonNull(input, "input must not be null");
		String canonical = input.toCanonicalString();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}

	public record DailyAthleteStateFingerprintInput(
			int baselineWindowDays,
			RecoveryAnalyticsCalculationVersion analyticsVersion,
			String recoveryCanonical,
			String priorObservationsCanonical,
			String metricsCanonical,
			String discomfortCanonical,
			String trainingCanonical,
			String scheduleCanonical) {

		public DailyAthleteStateFingerprintInput {
			Objects.requireNonNull(analyticsVersion, "analyticsVersion must not be null");
			recoveryCanonical = nullToEmpty(recoveryCanonical);
			priorObservationsCanonical = nullToEmpty(priorObservationsCanonical);
			metricsCanonical = nullToEmpty(metricsCanonical);
			discomfortCanonical = nullToEmpty(discomfortCanonical);
			trainingCanonical = nullToEmpty(trainingCanonical);
			scheduleCanonical = nullToEmpty(scheduleCanonical);
		}

		String toCanonicalString() {
			return String.join("\n",
					"baselineWindowDays=" + baselineWindowDays,
					"analyticsVersion=" + analyticsVersion.name(),
					"recovery=" + recoveryCanonical,
					"priors=" + priorObservationsCanonical,
					"metrics=" + metricsCanonical,
					"discomfort=" + discomfortCanonical,
					"training=" + trainingCanonical,
					"schedule=" + scheduleCanonical);
		}

		private static String nullToEmpty(String value) {
			return value == null ? "" : value;
		}
	}

	public static String joinSorted(List<String> lines) {
		return lines.stream().sorted().reduce((a, b) -> a + "|" + b).orElse("");
	}

	public static Comparator<DailyAthleteStateDiscomfortSnapshot> discomfortOrder() {
		return Comparator
				.comparing((DailyAthleteStateDiscomfortSnapshot d) -> d.bodyArea().name())
				.thenComparing(d -> d.bodySide().name())
				.thenComparingInt(DailyAthleteStateDiscomfortSnapshot::orderIndex);
	}

	public static Comparator<DailyAthleteStateRecoveryMetricSnapshot> metricOrder() {
		return Comparator.comparing(m -> m.metricType().name());
	}

	public static Comparator<DailyAthleteStateScheduledOccurrenceSnapshot> scheduleOrder() {
		return Comparator
				.comparing((DailyAthleteStateScheduledOccurrenceSnapshot s) -> s.occurrenceId().toString())
				.thenComparingInt(DailyAthleteStateScheduledOccurrenceSnapshot::orderIndex);
	}

}
