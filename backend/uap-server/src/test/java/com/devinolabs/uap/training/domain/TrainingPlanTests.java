package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class TrainingPlanTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void createsDraftPlanWithNormalizedNameAndDates() {
		TrainingPlan plan = TrainingPlan.create(
				TrainingPlanId.generate(),
				AthleteId.of(java.util.UUID.randomUUID()),
				TrainingPlanType.VERTICAL,
				null,
				"  Summer   Vertical  ",
				"  Build jump  ",
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 8, 31),
				null,
				null,
				CLOCK);

		assertThat(plan.name()).isEqualTo("Summer   Vertical");
		assertThat(plan.normalizedName()).isEqualTo("summer vertical");
		assertThat(plan.description()).isEqualTo("Build jump");
		assertThat(plan.status()).isEqualTo(TrainingPlanStatus.DRAFT);
		assertThat(plan.version()).isZero();
	}

	@Test
	void requiresCustomTypeNameForOtherOnly() {
		TrainingPlan other = TrainingPlan.create(
				TrainingPlanId.generate(),
				AthleteId.of(java.util.UUID.randomUUID()),
				TrainingPlanType.OTHER,
				"  Custom Block  ",
				"Custom Plan",
				null,
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 3, 1),
				null,
				null,
				CLOCK);
		assertThat(other.customTypeName()).isEqualTo("Custom Block");

		assertThatThrownBy(() -> TrainingPlan.create(
				TrainingPlanId.generate(),
				AthleteId.of(java.util.UUID.randomUUID()),
				TrainingPlanType.OTHER,
				" ",
				"Custom Plan",
				null,
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 3, 1),
				null,
				null,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> TrainingPlan.create(
				TrainingPlanId.generate(),
				AthleteId.of(java.util.UUID.randomUUID()),
				TrainingPlanType.STRENGTH,
				"Not allowed",
				"Strength",
				null,
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 3, 1),
				null,
				null,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsInvalidDatesAndBlankName() {
		assertThatThrownBy(() -> TrainingPlan.create(
				TrainingPlanId.generate(),
				AthleteId.of(java.util.UUID.randomUUID()),
				TrainingPlanType.STRENGTH,
				null,
				"Plan",
				null,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 7, 1),
				null,
				null,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> TrainingPlan.create(
				TrainingPlanId.generate(),
				AthleteId.of(java.util.UUID.randomUUID()),
				TrainingPlanType.STRENGTH,
				null,
				"  ",
				null,
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 2, 1),
				null,
				null,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void supportsValidLifecycleAndRejectsInvalidTransitions() {
		TrainingPlan plan = createDraft();
		plan.activate(LATER);
		assertThat(plan.status()).isEqualTo(TrainingPlanStatus.ACTIVE);

		plan.complete(LATER);
		assertThat(plan.status()).isEqualTo(TrainingPlanStatus.COMPLETED);

		plan.archive(LATER);
		assertThat(plan.status()).isEqualTo(TrainingPlanStatus.ARCHIVED);

		TrainingPlan draft = createDraft();
		assertThatThrownBy(() -> draft.complete(LATER)).isInstanceOf(IllegalStateException.class);

		TrainingPlan active = createDraft();
		active.activate(LATER);
		active.archive(LATER);
		assertThat(active.status()).isEqualTo(TrainingPlanStatus.ARCHIVED);

		TrainingPlan archivedFromDraft = createDraft();
		archivedFromDraft.archive(LATER);
		assertThatThrownBy(() -> archivedFromDraft.activate(LATER)).isInstanceOf(IllegalStateException.class);
	}

	private static TrainingPlan createDraft() {
		return TrainingPlan.create(
				TrainingPlanId.generate(),
				AthleteId.of(java.util.UUID.randomUUID()),
				TrainingPlanType.STRENGTH,
				null,
				"Strength Block",
				null,
				LocalDate.of(2026, 1, 1),
				LocalDate.of(2026, 3, 1),
				null,
				null,
				CLOCK);
	}

}
