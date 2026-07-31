package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class SessionRpeLoadTests {

	@Test
	void multipliesRpeByDurationMinutes() {
		SessionRpeLoad load = SessionRpeLoad.of(SessionRpe.of(8.0), 60);
		assertThat(load.value()).isEqualByComparingTo("480.00");
	}

	@Test
	void returnsNullWhenEitherInputIsMissing() {
		assertThat(SessionRpeLoad.ofNullable(null, 60)).isNull();
		assertThat(SessionRpeLoad.ofNullable(SessionRpe.of(8.0), null)).isNull();
	}

	@Test
	void rejectsInvalidDuration() {
		assertThatThrownBy(() -> SessionRpeLoad.of(SessionRpe.of(8.0), 0))
				.isInstanceOf(InvalidSessionDurationException.class);
		assertThatThrownBy(() -> SessionRpeLoad.of(SessionRpe.of(8.0), 1441))
				.isInstanceOf(InvalidSessionDurationException.class);
	}

}
