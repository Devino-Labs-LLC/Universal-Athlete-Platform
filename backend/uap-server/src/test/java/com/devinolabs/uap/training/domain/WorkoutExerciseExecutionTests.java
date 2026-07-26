package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkoutExerciseExecutionTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-25T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void fromPrescriptionSnapshotsExerciseFields() {
		WorkoutExercise exercise = WorkoutExercise.create(
				WorkoutExerciseId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				0,
				"Back Squat",
				ExerciseCategory.STRENGTH,
				ExerciseType.BARBELL,
				5,
				5,
				5,
				new BigDecimal("225"),
				WeightUnit.POUND,
				null,
				null,
				null,
				120,
				8,
				"3-0-1",
				"Brace",
				CLOCK);

		WorkoutExerciseExecution execution = WorkoutExerciseExecution.fromPrescription(
				exercise,
				WorkoutOccurrenceId.generate(),
				CLOCK);

		assertThat(execution.status()).isEqualTo(WorkoutExerciseExecutionStatus.NOT_STARTED);
		assertThat(execution.prescribedSets()).isEqualTo(5);
		assertThat(execution.prescribedMinimumReps()).isEqualTo(5);
		assertThat(execution.prescribedMaximumReps()).isEqualTo(5);
		assertThat(execution.prescribedTargetWeight()).isEqualByComparingTo("225");
		assertThat(execution.prescribedWeightUnit()).isEqualTo(WeightUnit.POUND);
		assertThat(execution.prescribedTempo()).isEqualTo("3-0-1");
		assertThat(execution.prescribedCoachingNotes()).isEqualTo("Brace");
	}

	@Test
	void lifecycleAndExecutionUpdates() {
		WorkoutExerciseExecution execution = fromSampleExercise();

		execution.start(CLOCK);
		assertThat(execution.status()).isEqualTo(WorkoutExerciseExecutionStatus.IN_PROGRESS);
		assertThat(execution.startedAt()).isEqualTo(CLOCK.instant());

		execution.applyDerivedActuals(
				5, 5, new BigDecimal("225"), WeightUnit.POUND, null, null, null, 90, new BigDecimal("8.00"), LATER);
		assertThat(execution.actualSets()).isEqualTo(5);
		assertThat(execution.actualReps()).isEqualTo(5);
		assertThat(execution.actualWeight()).isEqualByComparingTo("225");
		assertThat(execution.actualRpe()).isEqualByComparingTo("8.00");

		execution.updateNotes("  Strong set  ", LATER);
		assertThat(execution.athleteNotes()).isEqualTo("Strong set");

		execution.complete(LATER);
		assertThat(execution.status()).isEqualTo(WorkoutExerciseExecutionStatus.COMPLETED);
		assertThat(execution.completedAt()).isEqualTo(LATER.instant());
	}

	@Test
	void skipClearsCompletedAtAndRejectsInvalidActuals() {
		WorkoutExerciseExecution execution = fromSampleExercise();
		execution.skip(CLOCK);
		assertThat(execution.status()).isEqualTo(WorkoutExerciseExecutionStatus.SKIPPED);
		assertThat(execution.completedAt()).isNull();

		WorkoutExerciseExecution inProgress = fromSampleExercise();
		inProgress.start(CLOCK);
		inProgress.skip(LATER);
		assertThat(inProgress.completedAt()).isNull();

		WorkoutExerciseExecution updating = fromSampleExercise();
		assertThatThrownBy(() -> updating.applyDerivedActuals(
				-1, null, null, null, null, null, null, null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("actualSets");

		assertThatThrownBy(() -> updating.applyDerivedActuals(
				null, null, new BigDecimal("10"), null, null, null, null, null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("weightUnit");
	}

	private static WorkoutExerciseExecution fromSampleExercise() {
		WorkoutExercise exercise = WorkoutExercise.create(
				WorkoutExerciseId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				0,
				"Back Squat",
				ExerciseCategory.STRENGTH,
				ExerciseType.BARBELL,
				5,
				5,
				5,
				new BigDecimal("225"),
				WeightUnit.POUND,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				CLOCK);
		return WorkoutExerciseExecution.fromPrescription(exercise, WorkoutOccurrenceId.generate(), CLOCK);
	}

}
