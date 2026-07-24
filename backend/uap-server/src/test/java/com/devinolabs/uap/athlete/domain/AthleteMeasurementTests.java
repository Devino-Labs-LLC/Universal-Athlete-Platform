package com.devinolabs.uap.athlete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AthleteMeasurementTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void recordsValidMeasurementWithManualDefaultAndScale() {
		AthleteMeasurement measurement = AthleteMeasurement.record(
				AthleteMeasurementId.generate(),
				AthleteId.generate(),
				MeasurementType.BODY_WEIGHT,
				null,
				new BigDecimal("82.5"),
				MeasurementUnit.KILOGRAM,
				null,
				null,
				"  morning  ",
				Instant.parse("2026-07-24T14:00:00Z"),
				null,
				null,
				CLOCK);

		assertThat(measurement.source()).isEqualTo(MeasurementSource.MANUAL);
		assertThat(measurement.value()).isEqualByComparingTo("82.5000");
		assertThat(measurement.notes()).isEqualTo("morning");
		assertThat(measurement.version()).isZero();
		assertThat(measurement.measurementType()).isEqualTo(MeasurementType.BODY_WEIGHT);
	}

	@Test
	void enforcesCustomNameAndCustomUnitRules() {
		AthleteMeasurement other = AthleteMeasurement.record(
				AthleteMeasurementId.generate(),
				AthleteId.generate(),
				MeasurementType.OTHER,
				" Grip strength ",
				new BigDecimal("40"),
				MeasurementUnit.OTHER,
				" kgf ",
				MeasurementSource.MANUAL,
				null,
				Instant.parse("2026-07-24T14:00:00Z"),
				null,
				null,
				CLOCK);
		assertThat(other.customMeasurementName()).isEqualTo("Grip strength");
		assertThat(other.customUnit()).isEqualTo("kgf");

		assertThatThrownBy(() -> AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.OTHER, null,
				new BigDecimal("1"), MeasurementUnit.SCORE, null, null, null,
				Instant.parse("2026-07-24T14:00:00Z"), null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.BODY_WEIGHT, "extra",
				new BigDecimal("80"), MeasurementUnit.KILOGRAM, null, null, null,
				Instant.parse("2026-07-24T14:00:00Z"), null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.BODY_WEIGHT, null,
				new BigDecimal("80"), MeasurementUnit.KILOGRAM, "kg", null, null,
				Instant.parse("2026-07-24T14:00:00Z"), null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void enforcesTypeUnitCompatibilityAndValueRanges() {
		assertThat(MeasurementTypeUnitCompatibility.isCompatible(
				MeasurementType.BODY_WEIGHT, MeasurementUnit.KILOGRAM)).isTrue();
		assertThat(MeasurementTypeUnitCompatibility.isCompatible(
				MeasurementType.BODY_WEIGHT, MeasurementUnit.PERCENT)).isFalse();

		assertThatThrownBy(() -> AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.BODY_WEIGHT, null,
				new BigDecimal("80"), MeasurementUnit.PERCENT, null, null, null,
				Instant.parse("2026-07-24T14:00:00Z"), null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.BODY_WEIGHT, null,
				BigDecimal.ZERO, MeasurementUnit.KILOGRAM, null, null, null,
				Instant.parse("2026-07-24T14:00:00Z"), null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.BODY_FAT_PERCENTAGE, null,
				new BigDecimal("101"), MeasurementUnit.PERCENT, null, null, null,
				Instant.parse("2026-07-24T14:00:00Z"), null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.SESSION_RPE, null,
				new BigDecimal("11"), MeasurementUnit.SCORE, null, null, null,
				Instant.parse("2026-07-24T14:00:00Z"), null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		AthleteMeasurement rpe = AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.SESSION_RPE, null,
				new BigDecimal("7"), MeasurementUnit.SCORE, null, null, null,
				Instant.parse("2026-07-24T14:00:00Z"), null, null, CLOCK);
		assertThat(rpe.value()).isEqualByComparingTo("7.0000");
	}

	@Test
	void rejectsExcessiveFutureMeasuredAtAndAllowsSmallSkew() {
		assertThatThrownBy(() -> AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.RESTING_HEART_RATE, null,
				new BigDecimal("60"), MeasurementUnit.BEATS_PER_MINUTE, null, null, null,
				Instant.parse("2026-07-24T15:06:00Z"), null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		AthleteMeasurement ok = AthleteMeasurement.record(
				AthleteMeasurementId.generate(), AthleteId.generate(), MeasurementType.RESTING_HEART_RATE, null,
				new BigDecimal("60"), MeasurementUnit.BEATS_PER_MINUTE, null, null, null,
				Instant.parse("2026-07-24T15:05:00Z"), null, null, CLOCK);
		assertThat(ok.measuredAt()).isEqualTo(Instant.parse("2026-07-24T15:05:00Z"));
	}

	@Test
	void supportsCorrectionsWithoutChangingImmutableFields() {
		AthleteMeasurement measurement = AthleteMeasurement.record(
				AthleteMeasurementId.generate(),
				AthleteId.generate(),
				MeasurementType.VERTICAL_JUMP,
				null,
				new BigDecimal("60"),
				MeasurementUnit.CENTIMETER,
				null,
				MeasurementSource.COACH,
				"baseline",
				Instant.parse("2026-07-24T12:00:00Z"),
				null,
				null,
				CLOCK);

		AthleteMeasurementId id = measurement.id();
		AthleteId athleteId = measurement.athleteId();
		Instant createdAt = measurement.createdAt();

		measurement.correctValue(new BigDecimal("62.25"), LATER);
		measurement.correctUnit(MeasurementUnit.INCH, null, LATER);
		measurement.correctMeasuredAt(Instant.parse("2026-07-24T12:30:00Z"), LATER);
		measurement.updateNotes(null, LATER);
		AthleteSportId sportId = AthleteSportId.generate();
		AthleteGoalId goalId = AthleteGoalId.generate();
		measurement.linkSport(sportId, LATER);
		measurement.linkGoal(goalId, LATER);
		measurement.unlinkSport(LATER);
		measurement.unlinkGoal(LATER);

		assertThat(measurement.id()).isEqualTo(id);
		assertThat(measurement.athleteId()).isEqualTo(athleteId);
		assertThat(measurement.measurementType()).isEqualTo(MeasurementType.VERTICAL_JUMP);
		assertThat(measurement.source()).isEqualTo(MeasurementSource.COACH);
		assertThat(measurement.createdAt()).isEqualTo(createdAt);
		assertThat(measurement.value()).isEqualByComparingTo("62.2500");
		assertThat(measurement.unit()).isEqualTo(MeasurementUnit.INCH);
		assertThat(measurement.notes()).isNull();
		assertThat(measurement.athleteSportId()).isNull();
		assertThat(measurement.athleteGoalId()).isNull();
		assertThat(measurement.updatedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));
	}

}
