package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class WorkoutExerciseSetTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-25T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void fromExecutionPrescriptionCopiesSnapshotAndStartsNotStarted() {
		WorkoutExerciseExecution execution = execution();

		WorkoutExerciseSet set = WorkoutExerciseSet.fromExecutionPrescription(execution, 2, 1, CLOCK);

		assertThat(set.setNumber()).isEqualTo(2);
		assertThat(set.displayOrder()).isEqualTo(1);
		assertThat(set.setType()).isEqualTo(WorkoutExerciseSetType.WORKING);
		assertThat(set.status()).isEqualTo(WorkoutExerciseSetStatus.NOT_STARTED);
		assertThat(set.workoutExerciseExecutionId()).isEqualTo(execution.id());
		assertThat(set.workoutOccurrenceId()).isEqualTo(execution.workoutOccurrenceId());
		assertThat(set.athleteId()).isEqualTo(execution.athleteId());
		assertThat(set.prescribedMinimumReps()).isEqualTo(5);
		assertThat(set.prescribedMaximumReps()).isEqualTo(5);
		assertThat(set.prescribedWeight()).isEqualByComparingTo("225");
		assertThat(set.prescribedWeightUnit()).isEqualTo(WeightUnit.POUND);
		assertThat(set.prescribedRestSeconds()).isEqualTo(120);
		assertThat(set.prescribedTargetRpe()).isEqualTo(8);
		assertThat(set.actualReps()).isNull();
		assertThat(set.startedAt()).isNull();
		assertThat(set.completedAt()).isNull();
		assertThat(set.version()).isZero();
	}

	@Test
	void startThenCompleteRecordsTimestamps() {
		WorkoutExerciseSet set = WorkoutExerciseSet.fromExecutionPrescription(execution(), 1, 0, CLOCK);

		set.start(CLOCK);
		assertThat(set.status()).isEqualTo(WorkoutExerciseSetStatus.IN_PROGRESS);
		assertThat(set.startedAt()).isEqualTo(CLOCK.instant());

		set.complete(LATER);
		assertThat(set.status()).isEqualTo(WorkoutExerciseSetStatus.COMPLETED);
		assertThat(set.startedAt()).isEqualTo(CLOCK.instant());
		assertThat(set.completedAt()).isEqualTo(LATER.instant());
	}

	@Test
	void completingFromNotStartedBackfillsStartedAt() {
		WorkoutExerciseSet set = WorkoutExerciseSet.fromExecutionPrescription(execution(), 1, 0, CLOCK);

		set.complete(LATER);

		assertThat(set.status()).isEqualTo(WorkoutExerciseSetStatus.COMPLETED);
		assertThat(set.startedAt()).isEqualTo(LATER.instant());
		assertThat(set.completedAt()).isEqualTo(LATER.instant());
	}

	@Test
	void skipClearsCompletedAtAndWorksFromEitherActiveStatus() {
		WorkoutExerciseSet fromNotStarted = WorkoutExerciseSet.fromExecutionPrescription(execution(), 1, 0, CLOCK);
		fromNotStarted.skip(CLOCK);
		assertThat(fromNotStarted.status()).isEqualTo(WorkoutExerciseSetStatus.SKIPPED);
		assertThat(fromNotStarted.completedAt()).isNull();

		WorkoutExerciseSet fromInProgress = WorkoutExerciseSet.fromExecutionPrescription(execution(), 1, 0, CLOCK);
		fromInProgress.start(CLOCK);
		fromInProgress.skip(LATER);
		assertThat(fromInProgress.status()).isEqualTo(WorkoutExerciseSetStatus.SKIPPED);
		assertThat(fromInProgress.completedAt()).isNull();
	}

	@Test
	void terminalSetsRejectReopeningAndMutation() {
		WorkoutExerciseSet completed = WorkoutExerciseSet.fromExecutionPrescription(execution(), 1, 0, CLOCK);
		completed.complete(CLOCK);

		assertThatThrownBy(() -> completed.start(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.skip(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.updateNotes("late", LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.changeSetType(WorkoutExerciseSetType.AMRAP, LATER))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.updateActuals(
				5, null, null, null, null, null, null, null, LATER))
				.isInstanceOf(IllegalStateException.class);

		WorkoutExerciseSet skipped = WorkoutExerciseSet.fromExecutionPrescription(execution(), 1, 0, CLOCK);
		skipped.skip(CLOCK);
		assertThatThrownBy(() -> skipped.complete(LATER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(skipped::requireMutable).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void completeAndSkipAreIdempotentWithinTheirOwnStatus() {
		WorkoutExerciseSet completed = WorkoutExerciseSet.fromExecutionPrescription(execution(), 1, 0, CLOCK);
		completed.complete(CLOCK);
		completed.complete(LATER);
		assertThat(completed.completedAt()).isEqualTo(CLOCK.instant());

		WorkoutExerciseSet skipped = WorkoutExerciseSet.fromExecutionPrescription(execution(), 1, 0, CLOCK);
		skipped.skip(CLOCK);
		skipped.skip(LATER);
		assertThat(skipped.status()).isEqualTo(WorkoutExerciseSetStatus.SKIPPED);
	}

	@Test
	void updateActualsAndNotesValidateInput() {
		WorkoutExerciseSet set = WorkoutExerciseSet.fromExecutionPrescription(execution(), 1, 0, CLOCK);
		set.start(CLOCK);

		set.updateActuals(
				5, new BigDecimal("225"), WeightUnit.POUND, null, null, null, 180, new BigDecimal("8.50"), LATER);
		assertThat(set.actualReps()).isEqualTo(5);
		assertThat(set.actualWeight()).isEqualByComparingTo("225");
		assertThat(set.actualWeightUnit()).isEqualTo(WeightUnit.POUND);
		assertThat(set.actualRestSeconds()).isEqualTo(180);
		assertThat(set.actualRpe()).isEqualByComparingTo("8.50");

		set.updateNotes("  felt strong  ", LATER);
		assertThat(set.athleteNotes()).isEqualTo("felt strong");
		set.updateNotes("   ", LATER);
		assertThat(set.athleteNotes()).isNull();

		assertThatThrownBy(() -> set.updateActuals(
				-1, null, null, null, null, null, null, null, LATER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("actualReps");
		assertThatThrownBy(() -> set.updateActuals(
				null, new BigDecimal("100"), null, null, null, null, null, null, LATER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("actualWeightUnit");
		assertThatThrownBy(() -> set.updateActuals(
				null, null, null, null, new BigDecimal("5"), null, null, null, LATER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("actualDistanceUnit");
		assertThatThrownBy(() -> set.updateActuals(
				null, null, null, null, null, null, null, new BigDecimal("10.01"), LATER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("actualRpe");
		assertThatThrownBy(() -> set.updateNotes("x".repeat(WorkoutExerciseSet.MAX_ATHLETE_NOTES_LENGTH + 1), LATER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("athleteNotes");
	}

	@Test
	void createAdditionalDefaultsToWorkingAndValidatesPrescription() {
		WorkoutExerciseExecution execution = execution();

		WorkoutExerciseSet defaulted = WorkoutExerciseSet.createAdditional(
				execution.id(),
				execution.workoutOccurrenceId(),
				execution.athleteId(),
				6,
				5,
				null,
				3,
				5,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				CLOCK);
		assertThat(defaulted.setType()).isEqualTo(WorkoutExerciseSetType.WORKING);
		assertThat(defaulted.status()).isEqualTo(WorkoutExerciseSetStatus.NOT_STARTED);

		assertThatThrownBy(() -> WorkoutExerciseSet.createAdditional(
				execution.id(),
				execution.workoutOccurrenceId(),
				execution.athleteId(),
				6,
				5,
				WorkoutExerciseSetType.DROP_SET,
				8,
				5,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("prescribedMaximumReps");

		assertThatThrownBy(() -> WorkoutExerciseSet.createAdditional(
				execution.id(),
				execution.workoutOccurrenceId(),
				execution.athleteId(),
				0,
				0,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("setNumber");
	}

	@Test
	void renumberingStaysAvailableOnTerminalSets() {
		WorkoutExerciseSet set = WorkoutExerciseSet.fromExecutionPrescription(execution(), 3, 2, CLOCK);
		set.complete(CLOCK);

		set.changeSetNumber(1, LATER);
		set.changeDisplayOrder(0, LATER);

		assertThat(set.setNumber()).isEqualTo(1);
		assertThat(set.displayOrder()).isZero();
		assertThatThrownBy(() -> set.changeDisplayOrder(-1, LATER)).isInstanceOf(IllegalArgumentException.class);
	}

	private static WorkoutExerciseExecution execution() {
		WorkoutExercise exercise = WorkoutExercise.create(
				WorkoutExerciseId.generate(),
				WorkoutDayId.generate(),
				AthleteId.of(UUID.randomUUID()),
				SystemExerciseDefinitions.BACK_SQUAT,
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
		return WorkoutExerciseExecution.fromPrescription(exercise, WorkoutOccurrenceId.generate(), CLOCK);
	}

}
