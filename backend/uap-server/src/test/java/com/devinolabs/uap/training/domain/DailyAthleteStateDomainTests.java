package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DailyAthleteStateDomainTests {

	@Test
	void fingerprintIsDeterministicAndIgnoresCollectionOrder() {
		var inputA = new DailyAthleteStateFingerprintCalculator.DailyAthleteStateFingerprintInput(
				7,
				RecoveryAnalyticsCalculationVersion.RECOVERY_ANALYTICS_V1,
				"recovery",
				"p1|p2",
				"FATIGUE:7:3.14:FAR_ABOVE_BASELINE:5:1.8:SUFFICIENT",
				"KNEE:LEFT:3:",
				"daily:1:2400",
				"occ-a:COMPLETED|occ-b:SCHEDULED");
		var inputB = new DailyAthleteStateFingerprintCalculator.DailyAthleteStateFingerprintInput(
				7,
				RecoveryAnalyticsCalculationVersion.RECOVERY_ANALYTICS_V1,
				"recovery",
				"p1|p2",
				"FATIGUE:7:3.14:FAR_ABOVE_BASELINE:5:1.8:SUFFICIENT",
				"KNEE:LEFT:3:",
				"daily:1:2400",
				"occ-a:COMPLETED|occ-b:SCHEDULED");
		assertThat(DailyAthleteStateFingerprintCalculator.calculate(inputA))
				.isEqualTo(DailyAthleteStateFingerprintCalculator.calculate(inputB));
	}

	@Test
	void fingerprintChangesWhenBaselineWindowChanges() {
		var seven = new DailyAthleteStateFingerprintCalculator.DailyAthleteStateFingerprintInput(
				7,
				RecoveryAnalyticsCalculationVersion.RECOVERY_ANALYTICS_V1,
				"recovery",
				"priors",
				"metrics",
				"",
				"training",
				"schedule");
		var fourteen = new DailyAthleteStateFingerprintCalculator.DailyAthleteStateFingerprintInput(
				14,
				RecoveryAnalyticsCalculationVersion.RECOVERY_ANALYTICS_V1,
				"recovery",
				"priors",
				"metrics",
				"",
				"training",
				"schedule");
		assertThat(DailyAthleteStateFingerprintCalculator.calculate(seven))
				.isNotEqualTo(DailyAthleteStateFingerprintCalculator.calculate(fourteen));
	}

	@Test
	void completenessResolverLabelsSourceAvailabilityOnly() {
		assertThat(DailyAthleteStateCompletenessResolver.resolve(true, false, false))
				.isEqualTo(DailyAthleteStateCompleteness.COMPLETE);
		assertThat(DailyAthleteStateCompletenessResolver.resolve(false, true, false))
				.isEqualTo(DailyAthleteStateCompleteness.PARTIAL);
		assertThat(DailyAthleteStateCompletenessResolver.resolve(false, false, true))
				.isEqualTo(DailyAthleteStateCompleteness.PARTIAL);
		assertThat(DailyAthleteStateCompletenessResolver.resolve(false, false, false))
				.isEqualTo(DailyAthleteStateCompleteness.MINIMAL);
	}

	@Test
	void joinSortedIsOrderIndependent() {
		assertThat(DailyAthleteStateFingerprintCalculator.joinSorted(List.of("b", "a", "c")))
				.isEqualTo(DailyAthleteStateFingerprintCalculator.joinSorted(List.of("c", "a", "b")));
	}

	@Test
	void metricDirectionMetadataRemainsDescriptiveOnly() {
		assertThat(RecoveryMetricType.SLEEP_DURATION.scaleDirection())
				.isEqualTo(RecoveryMetricDirection.NEUTRAL_DIRECTION);
		assertThat(RecoveryMetricType.FATIGUE.scaleDirection())
				.isEqualTo(RecoveryMetricDirection.LOWER_REPORTED_VALUE);
		assertThat(RecoveryMetricType.MOOD.scaleDirection())
				.isEqualTo(RecoveryMetricDirection.HIGHER_REPORTED_VALUE);
	}

}
