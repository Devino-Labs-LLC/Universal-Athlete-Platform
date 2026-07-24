package com.devinolabs.uap.athlete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class WeightTests {

	@Test
	void constructsValidKilogramWeights() {
		Weight weight = Weight.ofKilograms(new BigDecimal("82.35"));

		assertThat(weight.kilograms()).isEqualByComparingTo("82.35");
		assertThat(Weight.ofKilograms(0.01).kilograms()).isEqualByComparingTo("0.01");
		assertThat(Weight.ofKilograms(500).kilograms()).isEqualByComparingTo("500.00");
	}

	@Test
	void convertsPoundsToKilograms() {
		Weight weight = Weight.ofPounds(180);

		assertThat(weight.kilograms()).isEqualByComparingTo("81.65");
	}

	@Test
	void rejectsInvalidWeights() {
		assertThatThrownBy(() -> Weight.ofKilograms(0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Weight.ofKilograms(-1))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Weight.ofKilograms(500.01))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Weight.ofKilograms((BigDecimal) null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Weight.ofPounds(0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Weight.ofPounds(-5))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void equalityUsesKilogramValue() {
		assertThat(Weight.ofKilograms(80)).isEqualTo(Weight.ofKilograms(new BigDecimal("80.00")));
		assertThat(Weight.ofKilograms(80)).isNotEqualTo(Weight.ofKilograms(81));
	}

}
