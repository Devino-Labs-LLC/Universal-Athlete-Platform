package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ExerciseDefinitionTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-06T10:15:30Z"), ZoneOffset.UTC);

	private static final Clock LATER = Clock.fixed(Instant.parse("2026-05-04T10:15:30Z"), ZoneOffset.UTC);

	@Test
	void systemDefinitionsAreUnownedActiveAndKeyedByTheirOwnId() {
		ExerciseDefinition definition = ExerciseDefinition.createSystem(
				SystemExerciseDefinitions.BACK_SQUAT, "Back Squat", CLOCK);

		assertThat(definition.scope()).isEqualTo(ExerciseDefinitionScope.SYSTEM);
		assertThat(definition.athleteId()).isNull();
		assertThat(definition.active()).isTrue();
		assertThat(definition.archivedAt()).isNull();
		assertThat(definition.normalizedName()).isEqualTo("back squat");
		assertThat(definition.performanceKey())
				.isEqualTo(ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT));
	}

	@Test
	void customDefinitionsTrimTheDisplayNameAndCollapseTheComparisonForm() {
		ExerciseDefinition definition = custom("  Bulgarian   Split Squat  ");

		assertThat(definition.canonicalName()).isEqualTo("Bulgarian   Split Squat");
		assertThat(definition.normalizedName()).isEqualTo("bulgarian split squat");
		assertThat(definition.isOwnedBy(definition.athleteId())).isTrue();
		assertThat(definition.isOwnedBy(AthleteId.of(UUID.randomUUID()))).isFalse();
	}

	@Test
	void renamingKeepsTheIdentitySoHistoryStaysAttached() {
		ExerciseDefinition definition = custom("Bulgarian Split Squat");
		ExerciseDefinitionId id = definition.id();

		definition.rename("Rear Foot Elevated Split Squat", LATER);

		assertThat(definition.id()).isEqualTo(id);
		assertThat(definition.canonicalName()).isEqualTo("Rear Foot Elevated Split Squat");
		assertThat(definition.normalizedName()).isEqualTo("rear foot elevated split squat");
		assertThat(definition.updatedAt()).isEqualTo(Instant.parse("2026-05-04T10:15:30Z"));
		assertThat(definition.createdAt()).isEqualTo(Instant.parse("2026-04-06T10:15:30Z"));
	}

	@Test
	void archivingDeactivatesOnceAndIsSafeToRepeat() {
		ExerciseDefinition definition = custom("Sled Push");

		definition.archive(LATER);
		Instant archivedAt = definition.archivedAt();
		definition.archive(Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC));

		assertThat(definition.active()).isFalse();
		assertThat(definition.archivedAt()).isEqualTo(archivedAt).isNotNull();
	}

	@Test
	void systemDefinitionsRejectRenamesAndArchiving() {
		ExerciseDefinition definition = ExerciseDefinition.createSystem(
				SystemExerciseDefinitions.BENCH_PRESS, "Bench Press", CLOCK);

		assertThatThrownBy(() -> definition.rename("Barbell Bench Press", LATER))
				.isInstanceOf(SystemExerciseDefinitionModificationNotAllowedException.class);
		assertThatThrownBy(() -> definition.archive(LATER))
				.isInstanceOf(SystemExerciseDefinitionModificationNotAllowedException.class);
	}

	@Test
	void namesShorterThanTwoCharactersOrLongerThanTheColumnAreRejected() {
		assertThatThrownBy(() -> custom(" "))
				.isInstanceOf(InvalidExerciseDefinitionNameException.class);
		assertThatThrownBy(() -> custom("A"))
				.isInstanceOf(InvalidExerciseDefinitionNameException.class);
		assertThatThrownBy(() -> custom("x".repeat(151)))
				.isInstanceOf(InvalidExerciseDefinitionNameException.class);
	}

	@Test
	void scopeAndOwnershipMustAgreeAndArchivedRowsMustCarryATimestamp() {
		assertThatThrownBy(() -> ExerciseDefinition.rehydrate(
				ExerciseDefinitionId.generate(), ExerciseDefinitionScope.SYSTEM, AthleteId.of(UUID.randomUUID()),
				"Back Squat", "back squat", true, null, Instant.EPOCH, Instant.EPOCH, 0L))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ExerciseDefinition.rehydrate(
				ExerciseDefinitionId.generate(), ExerciseDefinitionScope.ATHLETE_CUSTOM, null,
				"Back Squat", "back squat", true, null, Instant.EPOCH, Instant.EPOCH, 0L))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ExerciseDefinition.rehydrate(
				ExerciseDefinitionId.generate(), ExerciseDefinitionScope.ATHLETE_CUSTOM, AthleteId.of(UUID.randomUUID()),
				"Back Squat", "back squat", false, null, Instant.EPOCH, Instant.EPOCH, 0L))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ExerciseDefinition.rehydrate(
				ExerciseDefinitionId.generate(), ExerciseDefinitionScope.ATHLETE_CUSTOM, AthleteId.of(UUID.randomUUID()),
				"Back Squat", "back squat", true, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH, 0L))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static ExerciseDefinition custom(String canonicalName) {
		return ExerciseDefinition.createAthleteCustom(
				ExerciseDefinitionId.generate(), AthleteId.of(UUID.randomUUID()), canonicalName, CLOCK);
	}

}
