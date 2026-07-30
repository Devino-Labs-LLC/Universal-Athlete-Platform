package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class TrainingEnvironmentTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-06T10:15:30Z"), ZoneOffset.UTC);

	@Test
	void createNormalizesNameAndOrdersEquipmentUniquely() {
		TrainingEnvironment environment = TrainingEnvironment.create(
				TrainingEnvironmentId.generate(),
				AthleteId.of(UUID.randomUUID()),
				"  Home Gym  ",
				TrainingEnvironmentType.HOME_GYM,
				List.of(EquipmentType.BENCH, EquipmentType.DUMBBELL),
				null,
				null,
				true,
				CLOCK);

		assertThat(environment.name()).isEqualTo("Home Gym");
		assertThat(environment.normalizedName()).isEqualTo("home gym");
		assertThat(environment.availableEquipment()).containsExactly(
				EquipmentType.DUMBBELL,
				EquipmentType.BENCH);
	}

	@Test
	void renameUpdatesNormalizedNameWithoutChangingIdentity() {
		TrainingEnvironment environment = environment("Home Gym");
		TrainingEnvironmentId id = environment.id();

		environment.rename("Garage Gym", CLOCK);

		assertThat(environment.id()).isEqualTo(id);
		assertThat(environment.name()).isEqualTo("Garage Gym");
		assertThat(environment.normalizedName()).isEqualTo("garage gym");
	}

	@Test
	void replaceAvailableEquipmentReplacesTheWholeCollection() {
		TrainingEnvironment environment = environment("Home Gym");

		environment.replaceAvailableEquipment(List.of(EquipmentType.BARBELL), CLOCK);

		assertThat(environment.availableEquipment()).containsExactly(EquipmentType.BARBELL);
	}

	@Test
	void archiveClearsDefaultAndIsIdempotent() {
		TrainingEnvironment environment = environment("Home Gym");
		environment.markDefault(CLOCK);

		environment.archive(CLOCK);
		Instant archivedAt = environment.archivedAt();
		environment.archive(Clock.fixed(Instant.parse("2026-05-01T00:00:00Z"), ZoneOffset.UTC));

		assertThat(environment.active()).isFalse();
		assertThat(environment.defaultEnvironment()).isFalse();
		assertThat(environment.archivedAt()).isEqualTo(archivedAt);
	}

	@Test
	void archivedEnvironmentCannotBeMutated() {
		TrainingEnvironment environment = environment("Home Gym");
		environment.archive(CLOCK);

		assertThatThrownBy(() -> environment.rename("New Name", CLOCK))
				.isInstanceOf(TrainingEnvironmentArchivedException.class);
	}

	@Test
	void duplicateEquipmentIsRejected() {
		assertThatThrownBy(() -> TrainingEnvironment.create(
				TrainingEnvironmentId.generate(),
				AthleteId.of(UUID.randomUUID()),
				"Home Gym",
				TrainingEnvironmentType.HOME_GYM,
				List.of(EquipmentType.DUMBBELL, EquipmentType.DUMBBELL),
				null,
				null,
				false,
				CLOCK)).isInstanceOf(InvalidTrainingEnvironmentEquipmentException.class);
	}

	private static TrainingEnvironment environment(String name) {
		return TrainingEnvironment.create(
				TrainingEnvironmentId.generate(),
				AthleteId.of(UUID.randomUUID()),
				name,
				TrainingEnvironmentType.HOME_GYM,
				List.of(EquipmentType.DUMBBELL),
				null,
				null,
				false,
				CLOCK);
	}

}
