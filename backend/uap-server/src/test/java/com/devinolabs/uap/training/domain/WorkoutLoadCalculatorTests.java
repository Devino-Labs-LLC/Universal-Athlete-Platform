package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.ExerciseDefinitionMetadataFixtures;

class WorkoutLoadCalculatorTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T15:00:00Z"), ZoneOffset.UTC);
	private static final Instant NOW = CLOCK.instant();
	private static final AthleteId ATHLETE_ID = AthleteId.of(UUID.randomUUID());
	private static final WorkoutOccurrenceId OCCURRENCE_ID = WorkoutOccurrenceId.generate();
	private static final WorkoutExerciseExecutionId BACK_SQUAT_EXECUTION_ID = WorkoutExerciseExecutionId.generate();
	private static final WorkoutExerciseExecutionId FRONT_SQUAT_EXECUTION_ID = WorkoutExerciseExecutionId.generate();
	private static final WorkoutExerciseExecutionId RUNNING_EXECUTION_ID = WorkoutExerciseExecutionId.generate();
	private static final WorkoutExerciseExecutionId PLANK_EXECUTION_ID = WorkoutExerciseExecutionId.generate();

	@Test
	void calculatesObjectiveLoadFromCompletedExecutionsAndSets() {
		WorkoutExerciseExecution backSquat = execution(
				BACK_SQUAT_EXECUTION_ID,
				SystemExerciseDefinitions.BACK_SQUAT,
				"Back Squat",
				ExerciseDefinitionMetadataFixtures.backSquat(),
				1,
				false);
		WorkoutExerciseExecution frontToGoblet = execution(
				FRONT_SQUAT_EXECUTION_ID,
				SystemExerciseDefinitions.FRONT_SQUAT,
				"Front Squat",
				ExerciseDefinitionMetadataFixtures.backSquat(),
				2,
				true);
		WorkoutExerciseExecution running = execution(
				RUNNING_EXECUTION_ID,
				SystemExerciseDefinitions.RUNNING,
				"Running",
				cardioMetadata(),
				3,
				false);
		WorkoutExerciseExecution plank = execution(
				PLANK_EXECUTION_ID,
				SystemExerciseDefinitions.PLANK,
				"Plank",
				plankMetadata(),
				4,
				false);

		WorkoutLoadCalculator.Result result = WorkoutLoadCalculator.calculate(new WorkoutLoadCalculator.Input(
				ATHLETE_ID,
				TrainingPlanId.generate(),
				WorkoutDayId.generate(),
				OCCURRENCE_ID,
				LocalDate.of(2026, 7, 25),
				List.of(backSquat, frontToGoblet, running, plank),
				Map.of(
						backSquat.id(), strengthSets(backSquat.id(), 100, 5, 3),
						frontToGoblet.id(), strengthSets(frontToGoblet.id(), 30, 10, 3),
						running.id(), List.of(distanceSet(running.id(), "5000", 1800)),
						plank.id(), List.of(durationSet(plank.id(), 120))),
				null,
				sampleOccurrence(),
				NOW));

		assertThat(result.prescribedExerciseCount()).isEqualTo(4);
		assertThat(result.completedExerciseCount()).isEqualTo(4);
		assertThat(result.substitutedExerciseCount()).isEqualTo(1);
		assertThat(result.totalVolumeKilograms()).isEqualByComparingTo("2400.000");
		assertThat(result.totalDistanceMeters()).isEqualByComparingTo("5000.000");
		assertThat(result.totalDurationSeconds()).isEqualTo(1920);
		assertThat(result.lowImpactExerciseCount()).isEqualTo(2);
		assertThat(result.moderateImpactExerciseCount()).isEqualTo(1);
		assertThat(result.noImpactExerciseCount()).isEqualTo(1);
		assertThat(result.calculationVersion()).isEqualTo(TrainingLoadCalculationVersion.V1);
	}

	@Test
	void includesSessionRpeLoadWhenEffortProvided() {
		WorkoutExerciseExecution squat = execution(
				BACK_SQUAT_EXECUTION_ID,
				SystemExerciseDefinitions.BACK_SQUAT,
				"Back Squat",
				ExerciseDefinitionMetadataFixtures.backSquat(),
				1,
				false);
		WorkoutSessionEffort effort = WorkoutSessionEffort.create(
				ATHLETE_ID,
				TrainingPlanId.generate(),
				WorkoutDayId.generate(),
				OCCURRENCE_ID,
				SessionRpe.of(8.0),
				60,
				null,
				sampleOccurrence(),
				SessionEffortSource.ATHLETE_REPORTED,
				CLOCK);

		WorkoutLoadCalculator.Result result = WorkoutLoadCalculator.calculate(new WorkoutLoadCalculator.Input(
				ATHLETE_ID,
				TrainingPlanId.generate(),
				WorkoutDayId.generate(),
				OCCURRENCE_ID,
				LocalDate.of(2026, 7, 25),
				List.of(squat),
				Map.of(squat.id(), strengthSets(squat.id(), 100, 5, 1)),
				effort,
				sampleOccurrence(),
				NOW));

		assertThat(result.sessionRpe()).isEqualTo(SessionRpe.of(8.0));
		assertThat(result.sessionDurationMinutes()).isEqualTo(60);
		assertThat(result.sessionRpeLoad().value()).isEqualByComparingTo("480.00");
	}

	private static WorkoutOccurrence sampleOccurrence() {
		return WorkoutOccurrence.createManual(
				OCCURRENCE_ID,
				TrainingPlanId.generate(),
				WorkoutDayId.generate(),
				ATHLETE_ID,
				LocalDate.of(2026, 7, 25),
				null,
				null,
				CLOCK);
	}

	private static WorkoutExerciseExecution execution(
			WorkoutExerciseExecutionId id,
			ExerciseDefinitionId prescribedId,
			String name,
			ExerciseDefinitionMetadata metadata,
			int order,
			boolean substitutedToGoblet) {
		ExerciseDefinitionId performedId = substitutedToGoblet
				? SystemExerciseDefinitions.GOBLET_SQUAT
				: prescribedId;
		String performedName = substitutedToGoblet ? "Goblet Squat" : name;
		ExerciseDefinitionMetadata performedMetadata = substitutedToGoblet
				? ExerciseDefinitionMetadataFixtures.gobletSquat()
				: metadata;
		return WorkoutExerciseExecution.rehydrate(
				id,
				OCCURRENCE_ID,
				WorkoutExerciseId.generate(),
				prescribedId,
				name,
				performedId,
				performedName,
				performedMetadata.category(),
				performedMetadata.primaryMovementPattern(),
				performedMetadata.impactLevel(),
				ExercisePerformanceKey.of(performedId),
				substitutedToGoblet ? ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE : null,
				null,
				substitutedToGoblet ? NOW : null,
				ATHLETE_ID,
				order,
				ExerciseCategory.STRENGTH,
				ExerciseType.BARBELL,
				3,
				5,
				5,
				new BigDecimal("100"),
				WeightUnit.KILOGRAM,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				WorkoutExerciseExecutionStatus.COMPLETED,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				NOW,
				NOW,
				null,
				NOW,
				NOW,
				0L);
	}

	private static List<WorkoutExerciseSet> strengthSets(
			WorkoutExerciseExecutionId executionId,
			int weight,
			int reps,
			int count) {
		return java.util.stream.IntStream.rangeClosed(1, count)
				.mapToObj(setNumber -> WorkoutExerciseSet.rehydrate(
						WorkoutExerciseSetId.generate(),
						executionId,
						OCCURRENCE_ID,
						ATHLETE_ID,
						setNumber,
						setNumber - 1,
						WorkoutExerciseSetType.WORKING,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						null,
						reps,
						new BigDecimal(weight),
						WeightUnit.KILOGRAM,
						null,
						null,
						null,
						null,
						null,
						WorkoutExerciseSetStatus.COMPLETED,
						NOW,
						NOW,
						null,
						NOW,
						NOW,
						0L))
				.toList();
	}

	private static WorkoutExerciseSet distanceSet(WorkoutExerciseExecutionId executionId, String meters, int seconds) {
		return WorkoutExerciseSet.rehydrate(
				WorkoutExerciseSetId.generate(),
				executionId,
				OCCURRENCE_ID,
				ATHLETE_ID,
				1,
				0,
				WorkoutExerciseSetType.WORKING,
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
				seconds,
				new BigDecimal(meters),
				DistanceUnit.METER,
				null,
				null,
				WorkoutExerciseSetStatus.COMPLETED,
				NOW,
				NOW,
				null,
				NOW,
				NOW,
				0L);
	}

	private static WorkoutExerciseSet durationSet(WorkoutExerciseExecutionId executionId, int seconds) {
		return WorkoutExerciseSet.rehydrate(
				WorkoutExerciseSetId.generate(),
				executionId,
				OCCURRENCE_ID,
				ATHLETE_ID,
				1,
				0,
				WorkoutExerciseSetType.WORKING,
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
				seconds,
				null,
				null,
				null,
				null,
				WorkoutExerciseSetStatus.COMPLETED,
				NOW,
				NOW,
				null,
				NOW,
				NOW,
				0L);
	}

	private static ExerciseDefinitionMetadata cardioMetadata() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.ENDURANCE,
				ExerciseMetricMode.DISTANCE_AND_DURATION,
				MovementPattern.GAIT,
				List.of(),
				List.of(MuscleGroup.QUADRICEPS),
				List.of(),
				List.of(),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.OPEN_CHAIN,
				ImpactLevel.MODERATE_IMPACT,
				ExerciseDifficulty.BEGINNER);
	}

	private static ExerciseDefinitionMetadata plankMetadata() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STABILITY,
				ExerciseMetricMode.DURATION,
				MovementPattern.ISOMETRIC,
				List.of(),
				List.of(MuscleGroup.ABDOMINALS),
				List.of(),
				List.of(),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.CLOSED_CHAIN,
				ImpactLevel.NO_IMPACT,
				ExerciseDifficulty.BEGINNER);
	}

}
