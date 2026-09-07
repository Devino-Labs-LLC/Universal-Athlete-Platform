package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrainingAssignmentTests {

	private final Clock clock = Clock.fixed(Instant.parse("2026-09-07T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void declineAndUnableAreIndependentOfExecutionAndIdempotent() {
		TrainingAssignment assignment = sample();
		assignment.decline("Not today", clock);
		assertThat(assignment.status()).isEqualTo(TrainingAssignmentStatus.DECLINED);
		assignment.decline("Not today", clock);
		assertThat(assignment.status()).isEqualTo(TrainingAssignmentStatus.DECLINED);

		TrainingAssignment unable = sample();
		unable.markUnable("Travel", clock);
		assertThat(unable.status()).isEqualTo(TrainingAssignmentStatus.UNABLE);
		assertThatThrownBy(() -> unable.decline("Later", clock)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> unable.updateContent(0, "Other", null, LocalDate.parse("2026-09-08"), clock))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void sameIntentIgnoresWhitespaceAndRejectsBlankTitle() {
		TrainingAssignment assignment = sample();
		assertThat(assignment.sameIntent(" Tempo intervals ", null, LocalDate.parse("2026-09-07"))).isTrue();
		assertThatThrownBy(() -> TrainingAssignment.assign(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				"COACH",
				" ",
				null,
				LocalDate.parse("2026-09-07"),
				"key",
				clock)).isInstanceOf(IllegalArgumentException.class);
	}

	private TrainingAssignment sample() {
		return TrainingAssignment.assign(
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				"COACH",
				"Tempo intervals",
				null,
				LocalDate.parse("2026-09-07"),
				"key-1",
				clock);
	}

}
