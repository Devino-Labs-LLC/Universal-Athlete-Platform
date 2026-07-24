package com.devinolabs.uap.athlete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class HeightTests {

	@Test
	void constructsValidCentimeterHeights() {
		Height height = Height.ofCentimeters(new BigDecimal("180.50"));

		assertThat(height.centimeters()).isEqualByComparingTo("180.50");
		assertThat(Height.ofCentimeters(40).centimeters()).isEqualByComparingTo("40.00");
		assertThat(Height.ofCentimeters(300).centimeters()).isEqualByComparingTo("300.00");
	}

	@Test
	void convertsFeetInchesToCentimeters() {
		Height height = Height.ofFeetInches(5, 11);

		assertThat(height.centimeters()).isEqualByComparingTo("180.34");
	}

	@Test
	void rejectsInvalidHeights() {
		assertThatThrownBy(() -> Height.ofCentimeters(0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Height.ofCentimeters(-10))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Height.ofCentimeters(39.99))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Height.ofCentimeters(300.01))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Height.ofCentimeters((BigDecimal) null))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Height.ofFeetInches(-1, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Height.ofFeetInches(5, 12))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void equalityUsesCentimeterValue() {
		assertThat(Height.ofCentimeters(180)).isEqualTo(Height.ofCentimeters(new BigDecimal("180.00")));
		assertThat(Height.ofCentimeters(180)).isNotEqualTo(Height.ofCentimeters(181));
	}

}
