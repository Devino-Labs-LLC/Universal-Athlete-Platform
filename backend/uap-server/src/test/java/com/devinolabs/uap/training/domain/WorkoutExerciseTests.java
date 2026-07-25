package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkoutExerciseTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-25T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void createsPlannedExerciseWithNormalizedNameAndPrescription() {
		WorkoutExercise exercise = WorkoutExercise.create(
				WorkoutExerciseId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				0,
				"  Back   Squat  ",
				ExerciseCategory.STRENGTH,
				ExerciseType.BARBELL,
				3,
				5,
				8,
				new BigDecimal("100.5"),
				WeightUnit.KILOGRAM,
				null,
				null,
				null,
				90,
				8,
				"3-1-1",
				"  Keep chest up  ",
				CLOCK);

		assertThat(exercise.exerciseName()).isEqualTo("Back   Squat");
		assertThat(exercise.normalizedExerciseName()).isEqualTo("back squat");
		assertThat(exercise.status()).isEqualTo(WorkoutExerciseStatus.PLANNED);
		assertThat(exercise.sets()).isEqualTo(3);
		assertThat(exercise.minimumReps()).isEqualTo(5);
		assertThat(exercise.maximumReps()).isEqualTo(8);
		assertThat(exercise.targetWeight()).isEqualByComparingTo("100.5");
		assertThat(exercise.weightUnit()).isEqualTo(WeightUnit.KILOGRAM);
		assertThat(exercise.coachingNotes()).isEqualTo("Keep chest up");
	}

	@Test
	void rejectsInvalidPrescriptionAndName() {
		assertThatThrownBy(() -> create(0, 5, 8, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("sets");

		assertThatThrownBy(() -> create(3, 0, 8, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("minimumReps");

		assertThatThrownBy(() -> create(3, 8, 5, null, null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("maximumReps");

		assertThatThrownBy(() -> create(3, 5, 8, new BigDecimal("-1"), WeightUnit.POUND))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("targetWeight");

		assertThatThrownBy(() -> create(3, 5, 8, new BigDecimal("10"), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("weightUnit");

		assertThatThrownBy(() -> WorkoutExercise.create(
				WorkoutExerciseId.generate(), WorkoutDayId.generate(), AthleteId.of(UUID.randomUUID()),
				0, "  ", ExerciseCategory.CARDIO, ExerciseType.RUN, 1,
				null, null, null, null, null, null, null, null, null, null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void supportsValidLifecycleAndRejectsInvalidTransitions() {
		WorkoutExercise exercise = createPlanned();
		exercise.activate(LATER);
		assertThat(exercise.status()).isEqualTo(WorkoutExerciseStatus.ACTIVE);
		exercise.complete(LATER);
		assertThat(exercise.status()).isEqualTo(WorkoutExerciseStatus.COMPLETED);

		WorkoutExercise skipFromPlanned = createPlanned();
		skipFromPlanned.skip(LATER);
		assertThat(skipFromPlanned.status()).isEqualTo(WorkoutExerciseStatus.SKIPPED);

		WorkoutExercise skipFromActive = createPlanned();
		skipFromActive.activate(LATER);
		skipFromActive.skip(LATER);
		assertThat(skipFromActive.status()).isEqualTo(WorkoutExerciseStatus.SKIPPED);

		WorkoutExercise planned = createPlanned();
		assertThatThrownBy(() -> planned.complete(LATER)).isInstanceOf(IllegalStateException.class);

		WorkoutExercise completed = createPlanned();
		completed.activate(LATER);
		completed.complete(LATER);
		assertThatThrownBy(() -> completed.skip(LATER)).isInstanceOf(IllegalStateException.class);
	}

	private static WorkoutExercise createPlanned() {
		return create(3, 5, 5, new BigDecimal("60"), WeightUnit.KILOGRAM);
	}

	private static WorkoutExercise create(
			Integer sets,
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal targetWeight,
			WeightUnit weightUnit) {
		return WorkoutExercise.create(
				WorkoutExerciseId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				0,
				"Back Squat",
				ExerciseCategory.STRENGTH,
				ExerciseType.BARBELL,
				sets,
				minimumReps,
				maximumReps,
				targetWeight,
				weightUnit,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				CLOCK);
	}

}
