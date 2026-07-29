package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.ExerciseDefinitionMetadataFixtures;

class WorkoutExerciseExecutionTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-25T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void fromPrescriptionSnapshotsExerciseFields() {
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

		WorkoutExerciseExecution execution = WorkoutExerciseExecution.fromPrescription(
				exercise,
				WorkoutOccurrenceId.generate(),
				CLOCK);

		assertThat(execution.status()).isEqualTo(WorkoutExerciseExecutionStatus.NOT_STARTED);
		assertThat(execution.prescribedExerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(execution.performedExerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(execution.prescribedExerciseNameSnapshot()).isEqualTo("Back Squat");
		assertThat(execution.performedExerciseNameSnapshot()).isEqualTo("Back Squat");
		assertThat(execution.isSubstituted()).isFalse();
		assertThat(execution.substitutionReason()).isNull();
		assertThat(execution.substitutedAt()).isNull();
		assertThat(execution.exercisePerformanceKey())
				.isEqualTo(ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT));
		assertThat(execution.exercisePerformanceKey().value()).isNotEqualTo(exercise.id().value());
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

	@Test
	void substitutionMovesOnlyThePerformedIdentity() {
		WorkoutExerciseExecution execution = fromSampleExercise();

		execution.substitute(gobletSquat(), ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE, "  No rack  ", LATER);

		assertThat(execution.isSubstituted()).isTrue();
		assertThat(execution.prescribedExerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(execution.prescribedExerciseNameSnapshot()).isEqualTo("Back Squat");
		assertThat(execution.performedExerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.GOBLET_SQUAT);
		assertThat(execution.performedExerciseNameSnapshot()).isEqualTo("Goblet Squat");
		assertThat(execution.exerciseName()).isEqualTo("Goblet Squat");
		assertThat(execution.exercisePerformanceKey())
				.isEqualTo(ExercisePerformanceKey.of(SystemExerciseDefinitions.GOBLET_SQUAT));
		assertThat(execution.substitutionReason()).isEqualTo(ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE);
		assertThat(execution.substitutionNotes()).isEqualTo("No rack");
		assertThat(execution.substitutedAt()).isEqualTo(LATER.instant());
		assertThat(execution.prescribedSets()).isEqualTo(5);
	}

	@Test
	void substitutionRequiresAUsableReasonAndADifferentMovement() {
		WorkoutExerciseExecution execution = fromSampleExercise();

		assertThatThrownBy(() -> execution.substitute(gobletSquat(), null, null, LATER))
				.isInstanceOf(InvalidExerciseSubstitutionReasonException.class);
		assertThatThrownBy(() -> execution.substitute(
				gobletSquat(), ExerciseSubstitutionReason.REVERSION, null, LATER))
				.isInstanceOf(InvalidExerciseSubstitutionReasonException.class);
		assertThatThrownBy(() -> execution.substitute(
				backSquat(), ExerciseSubstitutionReason.ATHLETE_PREFERENCE, null, LATER))
				.isInstanceOf(WorkoutExerciseAlreadyUsesDefinitionException.class);
		assertThat(execution.isSubstituted()).isFalse();
	}

	@Test
	void revertRestoresThePrescribedIdentityAndClearsSubstitutionDetails() {
		WorkoutExerciseExecution execution = fromSampleExercise();
		execution.substitute(gobletSquat(), ExerciseSubstitutionReason.INJURY, "Knee", LATER);

		execution.revertSubstitution(LATER);

		assertThat(execution.isSubstituted()).isFalse();
		assertThat(execution.performedExerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(execution.performedExerciseNameSnapshot()).isEqualTo("Back Squat");
		assertThat(execution.exercisePerformanceKey())
				.isEqualTo(ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT));
		assertThat(execution.substitutionReason()).isNull();
		assertThat(execution.substitutionNotes()).isNull();
		assertThat(execution.substitutedAt()).isNull();
		assertThatThrownBy(() -> execution.revertSubstitution(LATER))
				.isInstanceOf(WorkoutExerciseNotSubstitutedException.class);
	}

	@Test
	void substitutingBackToThePrescribedMovementLeavesTheExecutionUnsubstituted() {
		WorkoutExerciseExecution execution = fromSampleExercise();
		execution.substitute(gobletSquat(), ExerciseSubstitutionReason.INJURY, "Knee", LATER);

		execution.substitute(backSquat(), ExerciseSubstitutionReason.ATHLETE_PREFERENCE, "Feels fine", LATER);

		assertThat(execution.isSubstituted()).isFalse();
		assertThat(execution.substitutionReason()).isNull();
		assertThat(execution.substitutionNotes()).isNull();
		assertThat(execution.substitutedAt()).isNull();
	}

	@Test
	void substitutionIsOnlyOfferedWhileTheExecutionIsOpen() {
		WorkoutExerciseExecution execution = fromSampleExercise();
		assertThat(execution.isSubstitutable()).isTrue();
		execution.start(CLOCK);
		assertThat(execution.isSubstitutable()).isTrue();
		execution.complete(LATER);
		assertThat(execution.isSubstitutable()).isFalse();

		WorkoutExerciseExecution skipped = fromSampleExercise();
		skipped.skip(CLOCK);
		assertThat(skipped.isSubstitutable()).isFalse();
	}

	@Test
	void rehydrateRejectsIdentitiesThatContradictTheSubstitutionDetails() {
		WorkoutExerciseExecution execution = fromSampleExercise();

		assertThatThrownBy(() -> rehydrateWith(
				execution,
				SystemExerciseDefinitions.GOBLET_SQUAT,
				ExercisePerformanceKey.of(SystemExerciseDefinitions.GOBLET_SQUAT),
				null,
				null))
				.isInstanceOf(WorkoutExerciseSubstitutionIdentityConflictException.class);
		assertThatThrownBy(() -> rehydrateWith(
				execution,
				SystemExerciseDefinitions.BACK_SQUAT,
				ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT),
				ExerciseSubstitutionReason.INJURY,
				CLOCK.instant()))
				.isInstanceOf(WorkoutExerciseSubstitutionIdentityConflictException.class);
		assertThatThrownBy(() -> rehydrateWith(
				execution,
				SystemExerciseDefinitions.GOBLET_SQUAT,
				ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT),
				ExerciseSubstitutionReason.INJURY,
				CLOCK.instant()))
				.isInstanceOf(ExercisePerformanceIdentityConflictException.class);
	}

	private static WorkoutExerciseExecution rehydrateWith(
			WorkoutExerciseExecution execution,
			ExerciseDefinitionId performedExerciseDefinitionId,
			ExercisePerformanceKey exercisePerformanceKey,
			ExerciseSubstitutionReason substitutionReason,
			Instant substitutedAt) {
		return WorkoutExerciseExecution.rehydrate(
				execution.id(),
				execution.workoutOccurrenceId(),
				execution.sourceWorkoutExerciseId(),
				execution.prescribedExerciseDefinitionId(),
				execution.prescribedExerciseNameSnapshot(),
				performedExerciseDefinitionId,
				"Performed",
				exercisePerformanceKey,
				substitutionReason,
				null,
				substitutedAt,
				execution.athleteId(),
				execution.displayOrder(),
				execution.category(),
				execution.type(),
				execution.prescribedSets(),
				execution.prescribedMinimumReps(),
				execution.prescribedMaximumReps(),
				execution.prescribedTargetWeight(),
				execution.prescribedWeightUnit(),
				execution.prescribedTargetDurationSeconds(),
				execution.prescribedTargetDistance(),
				execution.prescribedDistanceUnit(),
				execution.prescribedTargetRestSeconds(),
				execution.prescribedTargetRpe(),
				execution.prescribedTempo(),
				execution.prescribedCoachingNotes(),
				execution.status(),
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
				null,
				null,
				execution.createdAt(),
				execution.updatedAt(),
				execution.version());
	}

	private static ExerciseDefinition backSquat() {
		return ExerciseDefinition.createSystem(
				SystemExerciseDefinitions.BACK_SQUAT,
				"Back Squat",
				ExerciseDefinitionMetadataFixtures.backSquat(),
				CLOCK);
	}

	private static ExerciseDefinition gobletSquat() {
		return ExerciseDefinition.createSystem(
				SystemExerciseDefinitions.GOBLET_SQUAT,
				"Goblet Squat",
				ExerciseDefinitionMetadataFixtures.gobletSquat(),
				CLOCK);
	}

	private static WorkoutExerciseExecution fromSampleExercise() {
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
				null,
				null,
				null,
				null,
				CLOCK);
		return WorkoutExerciseExecution.fromPrescription(exercise, WorkoutOccurrenceId.generate(), CLOCK);
	}

}
