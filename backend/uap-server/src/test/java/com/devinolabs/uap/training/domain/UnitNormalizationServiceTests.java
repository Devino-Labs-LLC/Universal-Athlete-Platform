package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnitNormalizationServiceTests {

	@Test
	void normalizesPoundsToKilogramsAtMeasurementScale() {
		NormalizedWeight normalized = UnitNormalizationService.normalizeWeight(
				new BigDecimal("225"), WeightUnit.POUND);

		assertThat(normalized.kilograms()).isEqualByComparingTo("102.0583");
		assertThat(normalized.kilograms().scale()).isEqualTo(UnitNormalizationService.MEASUREMENT_SCALE);
	}

	@Test
	void treatsKilogramsAsAlreadyCanonical() {
		assertThat(UnitNormalizationService.normalizeWeight(new BigDecimal("120"), WeightUnit.KILOGRAM).kilograms())
				.isEqualByComparingTo("120.0000");
	}

	@Test
	void poundsAndKilogramsOfTheSameLoadCompareEqual() {
		NormalizedWeight pounds = UnitNormalizationService.normalizeWeight(
				new BigDecimal("225"), WeightUnit.POUND);
		NormalizedWeight kilograms = UnitNormalizationService.normalizeWeight(
				new BigDecimal("102.0582825"), WeightUnit.KILOGRAM);

		assertThat(pounds).isEqualTo(kilograms);
		assertThat(pounds.compareTo(kilograms)).isZero();
	}

	@Test
	void denormalizesBackToTheLoggedUnit() {
		BigDecimal pounds = UnitNormalizationService.denormalizeWeight(
				new BigDecimal("102.0583"), WeightUnit.POUND);

		assertThat(pounds).isEqualByComparingTo("225.0000");
	}

	@Test
	void normalizesEverySupportedDistanceUnitToMeters() {
		assertThat(UnitNormalizationService.normalizeDistance(new BigDecimal("400"), DistanceUnit.METER).meters())
				.isEqualByComparingTo("400.0000");
		assertThat(UnitNormalizationService.normalizeDistance(new BigDecimal("5"), DistanceUnit.KILOMETER).meters())
				.isEqualByComparingTo("5000.0000");
		assertThat(UnitNormalizationService.normalizeDistance(new BigDecimal("1"), DistanceUnit.MILE).meters())
				.isEqualByComparingTo("1609.3440");
	}

	@Test
	void denormalizesDistanceBackToTheLoggedUnit() {
		assertThat(UnitNormalizationService.denormalizeDistance(new BigDecimal("1609.3440"), DistanceUnit.MILE))
				.isEqualByComparingTo("1.0000");
	}

	@Test
	void computesVolumeFromNormalizedWeightAndRepetitions() {
		SetVolume volume = UnitNormalizationService.volumeOf(new BigDecimal("225"), WeightUnit.POUND, 5);

		assertThat(volume).isNotNull();
		assertThat(volume.kilogramRepetitions()).isEqualByComparingTo("510.2915");
	}

	@Test
	void producesNoVolumeForBodyweightOnlyWork() {
		assertThat(UnitNormalizationService.volumeOf(null, null, 12)).isNull();
		assertThat(UnitNormalizationService.volumeOf(new BigDecimal("100"), WeightUnit.KILOGRAM, null)).isNull();
		assertThat(UnitNormalizationService.volumeOf(BigDecimal.ZERO, WeightUnit.KILOGRAM, 10)).isNull();
	}

	@Test
	void roundsAveragedRpeToTwoDecimals() {
		assertThat(UnitNormalizationService.toRpeScale(new BigDecimal("8.335"))).isEqualByComparingTo("8.34");
	}

	@Test
	void rejectsNegativeAndMissingMeasurements() {
		assertThatThrownBy(() -> UnitNormalizationService.normalizeWeight(new BigDecimal("-1"), WeightUnit.KILOGRAM))
				.isInstanceOf(InvalidPerformanceMeasurementException.class);
		assertThatThrownBy(() -> UnitNormalizationService.normalizeWeight(null, WeightUnit.KILOGRAM))
				.isInstanceOf(InvalidPerformanceMeasurementException.class);
		assertThatThrownBy(() -> UnitNormalizationService.normalizeDistance(new BigDecimal("5"), null))
				.isInstanceOf(InvalidPerformanceMeasurementException.class);
	}

	@Test
	void rejectsMissingUnitsWhenDenormalizing() {
		assertThatThrownBy(() -> UnitNormalizationService.denormalizeWeight(BigDecimal.ONE, null))
				.isInstanceOf(UnsupportedWeightUnitException.class);
		assertThatThrownBy(() -> UnitNormalizationService.denormalizeDistance(BigDecimal.ONE, null))
				.isInstanceOf(UnsupportedDistanceUnitException.class);
	}

}
