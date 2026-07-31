package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.ExerciseDefinitionMetadataFixtures;
import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingLoadCalculationVersion;
import com.devinolabs.uap.training.domain.TrainingLoadGranularity;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutTrainingLoadAcceptanceIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private UpdateWorkoutExerciseSetUseCase updateWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseSetUseCase completeWorkoutExerciseSetUseCase;

	@Autowired
	private SkipWorkoutExerciseSetUseCase skipWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;

	@Autowired
	private CompleteWorkoutOccurrenceUseCase completeWorkoutOccurrenceUseCase;

	@Autowired
	private SubstituteWorkoutExerciseExecutionUseCase substituteWorkoutExerciseExecutionUseCase;

	@Autowired
	private SubmitWorkoutSessionEffortUseCase submitWorkoutSessionEffortUseCase;

	@Autowired
	private UpdateWorkoutSessionEffortUseCase updateWorkoutSessionEffortUseCase;

	@Autowired
	private GetWorkoutOccurrenceLoadSummaryUseCase getWorkoutOccurrenceLoadSummaryUseCase;

	@Autowired
	private RecomputeWorkoutOccurrenceLoadUseCase recomputeWorkoutOccurrenceLoadUseCase;

	@Autowired
	private GetAthleteTrainingLoadHistoryUseCase getAthleteTrainingLoadHistoryUseCase;

	@Autowired
	private ListWorkoutSessionEffortRevisionsUseCase listWorkoutSessionEffortRevisionsUseCase;

	@Autowired
	private CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;

	@Autowired
	private UpdateAthleteExerciseDefinitionUseCase updateAthleteExerciseDefinitionUseCase;

	@Test
	void criticalMixedSessionScenarioCalculatesObjectiveLoadSessionRpeAndWeeklyHistory() {
		AccountId accountId = athlete();
		Prescription prescription = prescribeMixedSession(accountId);
		LocalDate scheduledDate = LocalDate.of(2026, 7, 25);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId,
				prescription.planId(),
				prescription.dayId(),
				scheduledDate,
				null,
				null);

		WorkoutExerciseExecutionId backSquatExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.BACK_SQUAT).id();
		WorkoutExerciseExecutionId frontSquatExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.FRONT_SQUAT).id();
		WorkoutExerciseExecutionId runningExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.RUNNING).id();
		WorkoutExerciseExecutionId plankExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.PLANK).id();

		substituteWorkoutExerciseExecutionUseCase.execute(
				accountId,
				prescription.planId(),
				prescription.dayId(),
				occurrence.occurrence().id(),
				frontSquatExecutionId,
				SystemExerciseDefinitions.GOBLET_SQUAT,
				ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE,
				"No rack",
				null);

		completeStrengthExercise(
				accountId, prescription, occurrence.occurrence().id(), backSquatExecutionId,
				new BigDecimal("100"), 5, 3);
		completeStrengthExercise(
				accountId, prescription, occurrence.occurrence().id(), frontSquatExecutionId,
				new BigDecimal("30"), 10, 3);
		completeDistanceExercise(
				accountId, prescription, occurrence.occurrence().id(), runningExecutionId,
				new BigDecimal("5000"), 1800);
		completeDurationExercise(
				accountId, prescription, occurrence.occurrence().id(), plankExecutionId, 120);

		completeWorkoutOccurrenceUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id());

		WorkoutOccurrenceLoadSummaryResult afterComplete = getWorkoutOccurrenceLoadSummaryUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id());
		assertObjectiveLoad(afterComplete, null, null);

		submitWorkoutSessionEffortUseCase.execute(
				accountId,
				prescription.planId(),
				prescription.dayId(),
				occurrence.occurrence().id(),
				new BigDecimal("8.0"),
				60,
				"Hard but manageable");

		WorkoutOccurrenceLoadSummaryResult afterSubmit = getWorkoutOccurrenceLoadSummaryUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id());
		assertObjectiveLoad(afterSubmit, new BigDecimal("8.0"), new BigDecimal("480.00"));

		updateWorkoutSessionEffortUseCase.execute(
				accountId,
				prescription.planId(),
				prescription.dayId(),
				occurrence.occurrence().id(),
				new BigDecimal("8.5"),
				65,
				"Felt heavier on review");

		WorkoutOccurrenceLoadSummaryResult afterUpdate = getWorkoutOccurrenceLoadSummaryUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id());
		assertObjectiveLoad(afterUpdate, new BigDecimal("8.5"), new BigDecimal("552.50"));
		assertThat(listWorkoutSessionEffortRevisionsUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id()))
				.hasSize(1);

		WorkoutOccurrenceLoadSummaryResult firstRecompute = recomputeWorkoutOccurrenceLoadUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id());
		WorkoutOccurrenceLoadSummaryResult secondRecompute = recomputeWorkoutOccurrenceLoadUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id());
		assertThat(firstRecompute.totalVolumeKilograms()).isEqualByComparingTo(secondRecompute.totalVolumeKilograms());
		assertThat(firstRecompute.sessionRpeLoad().value()).isEqualByComparingTo(secondRecompute.sessionRpeLoad().value());
		assertThat(firstRecompute.calculationVersion()).isEqualTo(TrainingLoadCalculationVersion.V1);

		TrainingLoadHistoryResult weeklyHistory = getAthleteTrainingLoadHistoryUseCase.execute(
				accountId,
				LocalDate.of(2026, 7, 20),
				LocalDate.of(2026, 7, 26),
				TrainingLoadGranularity.WEEKLY,
				null,
				null,
				null,
				null,
				null,
				null);
		assertThat(weeklyHistory.weeklySummaries()).hasSize(1);
		assertThat(weeklyHistory.weeklySummaries().getFirst().weekStartDate()).isEqualTo(LocalDate.of(2026, 7, 20));
		assertThat(weeklyHistory.weeklySummaries().getFirst().weekEndDate()).isEqualTo(LocalDate.of(2026, 7, 26));
		assertThat(weeklyHistory.weeklySummaries().getFirst().ratedOccurrenceCount()).isEqualTo(1);
		assertThat(weeklyHistory.weeklySummaries().getFirst().averageSessionRpe())
				.isEqualByComparingTo("8.5");
		assertThat(weeklyHistory.weeklySummaries().getFirst().totalSessionRpeLoad())
				.isEqualByComparingTo("552.50");
	}

	@Test
	void catalogReclassificationDoesNotRewriteHistoricalLoadSnapshots() {
		AccountId accountId = athlete();
		ExerciseDefinitionResult customSquat = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Catalog Squat", ExerciseDefinitionMetadataFixtures.hotelDumbbellSquat());
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Snapshot Block", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower", null, 1, DayOfWeek.MONDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), customSquat.id(), "Catalog Squat",
				ExerciseCategory.STRENGTH, ExerciseType.DUMBBELL,
				1, 8, 8, new BigDecimal("50"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), LocalDate.of(2026, 8, 3), null, null);
		WorkoutExerciseExecutionId executionId = occurrence.executions().getFirst().id();
		completeStrengthExercise(accountId, new Prescription(accountId, plan.id(), day.id(), null),
				occurrence.occurrence().id(), executionId, new BigDecimal("50"), 8, 1);
		completeWorkoutOccurrenceUseCase.execute(accountId, plan.id(), day.id(), occurrence.occurrence().id());

		WorkoutOccurrenceLoadSummaryResult beforeReclassify = getWorkoutOccurrenceLoadSummaryUseCase.execute(
				accountId, plan.id(), day.id(), occurrence.occurrence().id());
		assertThat(beforeReclassify.lowImpactExerciseCount()).isEqualTo(1);
		assertThat(beforeReclassify.highImpactExerciseCount()).isZero();

		updateAthleteExerciseDefinitionUseCase.execute(
				accountId,
				customSquat.id(),
				new UpdateAthleteExerciseDefinitionCommand(
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						ImpactLevel.HIGH_IMPACT, true,
						null, false));

		WorkoutOccurrenceLoadSummaryResult afterReclassify = recomputeWorkoutOccurrenceLoadUseCase.execute(
				accountId, plan.id(), day.id(), occurrence.occurrence().id());
		assertThat(afterReclassify.lowImpactExerciseCount()).isEqualTo(1);
		assertThat(afterReclassify.highImpactExerciseCount()).isZero();
		assertThat(afterReclassify.totalVolumeKilograms())
				.isEqualByComparingTo(beforeReclassify.totalVolumeKilograms());
	}

	@Test
	void sessionEffortRequiresCompletedOccurrenceAndIsUnique() {
		AccountId accountId = athlete();
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Effort Block", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Single", null, 1, DayOfWeek.SUNDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BACK_SQUAT, "Back Squat",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				1, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);
		Prescription prescription = new Prescription(accountId, plan.id(), day.id(), null);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), LocalDate.of(2026, 7, 26), null, null);

		assertThatThrownBy(() -> submitWorkoutSessionEffortUseCase.execute(
				accountId,
				prescription.planId(),
				prescription.dayId(),
				occurrence.occurrence().id(),
				new BigDecimal("7.0"),
				45,
				null))
				.isInstanceOf(WorkoutSessionEffortNotAllowedException.class);

		WorkoutExerciseExecutionId executionId = occurrence.executions().getFirst().id();
		completeStrengthExercise(
				accountId, prescription, occurrence.occurrence().id(), executionId,
				new BigDecimal("100"), 5, 1);
		completeWorkoutOccurrenceUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id());

		submitWorkoutSessionEffortUseCase.execute(
				accountId,
				prescription.planId(),
				prescription.dayId(),
				occurrence.occurrence().id(),
				new BigDecimal("7.0"),
				45,
				null);
		assertThatThrownBy(() -> submitWorkoutSessionEffortUseCase.execute(
				accountId,
				prescription.planId(),
				prescription.dayId(),
				occurrence.occurrence().id(),
				new BigDecimal("7.5"),
				50,
				null))
				.isInstanceOf(WorkoutSessionEffortAlreadyExistsException.class);
	}

	private static void assertObjectiveLoad(
			WorkoutOccurrenceLoadSummaryResult summary,
			BigDecimal sessionRpe,
			BigDecimal sessionRpeLoad) {
		assertThat(summary.prescribedExerciseCount()).isEqualTo(4);
		assertThat(summary.completedExerciseCount()).isEqualTo(4);
		assertThat(summary.substitutedExerciseCount()).isEqualTo(1);
		assertThat(summary.totalVolumeKilograms()).isEqualByComparingTo("2400.000");
		assertThat(summary.totalDistanceMeters()).isEqualByComparingTo("5000.000");
		assertThat(summary.totalDurationSeconds()).isEqualTo(1920);
		assertThat(summary.lowImpactExerciseCount()).isEqualTo(2);
		assertThat(summary.moderateImpactExerciseCount()).isEqualTo(1);
		assertThat(summary.noImpactExerciseCount()).isEqualTo(1);
		if (sessionRpe != null) {
			assertThat(summary.sessionRpe().value()).isEqualByComparingTo(sessionRpe);
		}
		if (sessionRpeLoad != null) {
			assertThat(summary.sessionRpeLoad().value()).isEqualByComparingTo(sessionRpeLoad);
		}
	}

	private Prescription prescribeMixedSession(AccountId accountId) {
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Load Block", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Mixed", null, 1, DayOfWeek.SATURDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BACK_SQUAT, "Back Squat",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, 0);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.FRONT_SQUAT, "Front Squat",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 10, 10, new BigDecimal("30"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, 1);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.RUNNING, "Running",
				ExerciseCategory.CARDIO, ExerciseType.RUN,
				1, null, null, null, null,
				null, null, null, null, null, null, null, 2);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.PLANK, "Plank",
				ExerciseCategory.STRENGTH, ExerciseType.BODYWEIGHT,
				1, null, null, null, null,
				120, null, null, null, null, null, null, 3);
		return new Prescription(accountId, plan.id(), day.id(), null);
	}

	private static WorkoutExerciseExecutionResult executionFor(
			WorkoutOccurrenceDetailResult occurrence,
			ExerciseDefinitionId prescribedDefinitionId) {
		return occurrence.executions().stream()
				.filter(execution -> execution.prescribedExerciseDefinitionId().equals(prescribedDefinitionId))
				.findFirst()
				.orElseThrow();
	}

	private void completeStrengthExercise(
			AccountId accountId,
			Prescription prescription,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			BigDecimal weight,
			int reps,
			int completedSetCount) {
		List<WorkoutExerciseSetResult> sets = listWorkoutExerciseSetsUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId);
		for (int index = 0; index < completedSetCount; index++) {
			WorkoutExerciseSetResult set = sets.get(index);
			updateWorkoutExerciseSetUseCase.execute(
					accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id(),
					new UpdateWorkoutExerciseSetCommand(
							null, false,
							reps, true,
							weight, true,
							WeightUnit.KILOGRAM, true,
							null, false,
							null, false,
							null, false,
							null, false,
							null, false,
							null, false));
			completeWorkoutExerciseSetUseCase.execute(
					accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id());
		}
		sets.stream().skip(completedSetCount).forEach(set -> skipWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id()));
		completeWorkoutExerciseExecutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId);
	}

	private void completeDistanceExercise(
			AccountId accountId,
			Prescription prescription,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			BigDecimal distanceMeters,
			int durationSeconds) {
		WorkoutExerciseSetResult set = listWorkoutExerciseSetsUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId).getFirst();
		updateWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id(),
				new UpdateWorkoutExerciseSetCommand(
						null, false,
						null, false,
						null, false,
						null, false,
						durationSeconds, true,
						distanceMeters, true,
						DistanceUnit.METER, true,
						null, false,
						null, false,
						null, false));
		completeWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id());
		completeWorkoutExerciseExecutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId);
	}

	private void completeDurationExercise(
			AccountId accountId,
			Prescription prescription,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			int durationSeconds) {
		WorkoutExerciseSetResult set = listWorkoutExerciseSetsUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId).getFirst();
		updateWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id(),
				new UpdateWorkoutExerciseSetCommand(
						null, false,
						null, false,
						null, false,
						null, false,
						durationSeconds, true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false));
		completeWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id());
		completeWorkoutExerciseExecutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Jordan",
				"Reed",
				LocalDate.of(1994, 3, 14),
				Sex.FEMALE,
				Height.ofCentimeters(168),
				Weight.ofKilograms(64),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		return accountId;
	}

	private record Prescription(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutExerciseId exerciseId) {
	}

}
