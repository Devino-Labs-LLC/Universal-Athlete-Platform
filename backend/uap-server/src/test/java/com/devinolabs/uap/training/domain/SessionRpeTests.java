package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class SessionRpeTests {

	@Test
	void acceptsValidValuesWithOneDecimalPlace() {
		assertThat(SessionRpe.of(new BigDecimal("8.0")).value()).isEqualByComparingTo("8.0");
		assertThat(SessionRpe.of(0.0).value()).isEqualByComparingTo("0.0");
		assertThat(SessionRpe.of(10.0).value()).isEqualByComparingTo("10.0");
	}

	@Test
	void rejectsOutOfRangeAndUnsupportedPrecision() {
		assertThatThrownBy(() -> SessionRpe.of(new BigDecimal("-0.1")))
				.isInstanceOf(InvalidSessionRpeException.class);
		assertThatThrownBy(() -> SessionRpe.of(new BigDecimal("10.1")))
				.isInstanceOf(InvalidSessionRpeException.class);
		assertThatThrownBy(() -> SessionRpe.of(new BigDecimal("7.55")))
				.isInstanceOf(InvalidSessionRpeException.class);
		assertThatThrownBy(() -> SessionRpe.of((BigDecimal) null))
				.isInstanceOf(NullPointerException.class);
	}

}
