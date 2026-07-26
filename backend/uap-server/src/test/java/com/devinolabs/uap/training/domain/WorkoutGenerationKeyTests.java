package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class WorkoutGenerationKeyTests {

	private static final LocalDate DATE = LocalDate.of(2026, 8, 10);

	@Test
	void keyIsDeterministicForTheSamePlacement() {
		TrainingPlanId planId = TrainingPlanId.generate();
		WorkoutDayId dayId = WorkoutDayId.generate();

		WorkoutGenerationKey first = WorkoutGenerationKey.of(planId, dayId, DATE, 1);
		WorkoutGenerationKey second = WorkoutGenerationKey.of(planId, dayId, DATE, 1);

		assertThat(first).isEqualTo(second);
		assertThat(first.value()).isEqualTo("%s|%s|2026-08-10|1".formatted(planId.value(), dayId.value()));
	}

	@Test
	void differentDatesOrCyclesProduceDifferentKeys() {
		TrainingPlanId planId = TrainingPlanId.generate();
		WorkoutDayId dayId = WorkoutDayId.generate();

		assertThat(WorkoutGenerationKey.of(planId, dayId, DATE, 1))
				.isNotEqualTo(WorkoutGenerationKey.of(planId, dayId, DATE.plusDays(1), 1));
		assertThat(WorkoutGenerationKey.of(planId, dayId, DATE, 1))
				.isNotEqualTo(WorkoutGenerationKey.of(planId, dayId, DATE, 2));
		assertThat(WorkoutGenerationKey.of(planId, dayId, DATE, 1))
				.isNotEqualTo(WorkoutGenerationKey.of(planId, WorkoutDayId.generate(), DATE, 1));
	}

	@Test
	void rejectsInvalidInputs() {
		TrainingPlanId planId = TrainingPlanId.generate();
		WorkoutDayId dayId = WorkoutDayId.generate();

		assertThatThrownBy(() -> WorkoutGenerationKey.of(planId, dayId, DATE, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new WorkoutGenerationKey("  "))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new WorkoutGenerationKey("x".repeat(WorkoutGenerationKey.MAX_LENGTH + 1)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(WorkoutGenerationKey.ofNullable(null)).isNull();
	}

}
