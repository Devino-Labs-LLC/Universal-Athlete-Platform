package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstimatedOneRepMaxCalculatorTests {

	@Test
	void appliesEpleyForMultiRepetitionSets() {
		NormalizedWeight estimate = EstimatedOneRepMaxCalculator.estimate(
				new BigDecimal("120"), WeightUnit.KILOGRAM, 3);

		assertThat(estimate).isNotNull();
		assertThat(estimate.kilograms()).isEqualByComparingTo("132.0000");
	}

	@Test
	void returnsTheLoadUnchangedForASingle() {
		NormalizedWeight estimate = EstimatedOneRepMaxCalculator.estimate(
				new BigDecimal("140"), WeightUnit.KILOGRAM, 1);

		assertThat(estimate.kilograms()).isEqualByComparingTo("140.0000");
	}

	@Test
	void normalizesPoundsBeforeEstimating() {
		NormalizedWeight estimate = EstimatedOneRepMaxCalculator.estimate(
				new BigDecimal("225"), WeightUnit.POUND, 5);

		assertThat(estimate.kilograms()).isEqualByComparingTo("119.0680");
		assertThat(UnitNormalizationService.denormalizeWeight(estimate.kilograms(), WeightUnit.POUND))
				.isEqualByComparingTo("262.5000");
	}

	@Test
	void refusesToEstimateOutsideTheUsableRepetitionRange() {
		assertThat(EstimatedOneRepMaxCalculator.estimate(new BigDecimal("60"), WeightUnit.KILOGRAM, 0)).isNull();
		assertThat(EstimatedOneRepMaxCalculator.estimate(
				new BigDecimal("60"),
				WeightUnit.KILOGRAM,
				EstimatedOneRepMaxCalculator.MAX_ESTIMABLE_REPETITIONS + 1)).isNull();
	}

	@Test
	void refusesToEstimateWithoutAnExternalLoad() {
		assertThat(EstimatedOneRepMaxCalculator.estimate(null, WeightUnit.KILOGRAM, 5)).isNull();
		assertThat(EstimatedOneRepMaxCalculator.estimate(new BigDecimal("60"), null, 5)).isNull();
		assertThat(EstimatedOneRepMaxCalculator.estimate(BigDecimal.ZERO, WeightUnit.KILOGRAM, 5)).isNull();
	}

}
