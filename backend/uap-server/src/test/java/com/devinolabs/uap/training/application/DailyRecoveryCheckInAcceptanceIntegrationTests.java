package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DailyRecoveryCheckInAcceptanceIntegrationTests {

	private static final LocalDate TRAINING_DATE = LocalDate.of(2026, 7, 31);
	private static final LocalDate REST_DATE = LocalDate.of(2026, 7, 30);

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
	private RebuildAthleteTrainingLoadUseCase rebuildAthleteTrainingLoadUseCase;

	@Autowired
	private CreateDailyRecoveryCheckInUseCase createDailyRecoveryCheckInUseCase;

	@Autowired
	private UpdateDailyRecoveryCheckInUseCase updateDailyRecoveryCheckInUseCase;

	@Autowired
	private GetRecoveryCheckInCalendarUseCase getRecoveryCheckInCalendarUseCase;

	@Autowired
	private GetDailyRecoveryCheckInUseCase getDailyRecoveryCheckInUseCase;

	@Autowired
	private ListDailyRecoveryCheckInRevisionsUseCase listDailyRecoveryCheckInRevisionsUseCase;

	@Autowired
	private GetAthleteRecoveryHistoryUseCase getAthleteRecoveryHistoryUseCase;

	@Test
	void criticalRecoveryCheckInScenarioWithTrainingLoadContext() {
		AccountId accountId = athlete();
		Prescription prescription = prescribeMixedSession(accountId);
		completeMixedSession(accountId, prescription, TRAINING_DATE);

		DailyRecoveryCheckInResult created = createDailyRecoveryCheckInUseCase.execute(
				accountId,
				TRAINING_DATE,
				420,
				3,
				4,
				3,
				2,
				4,
				3,
				List.of(new BodyAreaDiscomfortObservation.Input(
						"LOWER_BACK", "RIGHT", 2, "Mild tightness")),
				"Tired after yesterday's session");

		assertThat(created.fatigue().label()).isEqualTo("HIGH");
		assertThat(created.discomfortAreas()).hasSize(1);
		assertThat(created.discomfortAreas().getFirst().bodyArea().name()).isEqualTo("LOWER_BACK");

		LocalDate weekStart = LocalDate.of(2026, 7, 27);
		LocalDate weekEnd = LocalDate.of(2026, 8, 2);
		RecoveryCheckInCalendarResult calendar = getRecoveryCheckInCalendarUseCase.execute(
				accountId, weekStart, weekEnd);

		RecoveryCheckInCalendarDayResult trainingDay = calendar.days().stream()
				.filter(day -> day.date().equals(TRAINING_DATE))
				.findFirst()
				.orElseThrow();
		assertThat(trainingDay.checkInPresent()).isTrue();
		assertThat(trainingDay.checkIn().fatigue().value()).isEqualTo(4);
		assertThat(trainingDay.trainingLoad().occurrenceCount()).isEqualTo(1);
		assertThat(trainingDay.trainingLoad().totalVolumeKilograms()).isEqualByComparingTo("2400");
		assertThat(trainingDay.trainingLoad().totalSessionRpeLoad()).isEqualByComparingTo("552.50");
		assertThat(trainingDay.completedWorkoutCount()).isEqualTo(1);

		long absentDays = calendar.days().stream()
				.filter(day -> !day.date().equals(TRAINING_DATE))
				.filter(day -> !day.checkInPresent())
				.count();
		assertThat(absentDays).isEqualTo(6);

		DailyRecoveryCheckInResult updated = updateDailyRecoveryCheckInUseCase.execute(
				accountId,
				created.id(),
				new UpdateDailyRecoveryCheckInCommand(
						null, false,
						null, false,
						3, true,
						null, false,
						null, false,
						null, false,
						4, true,
						List.of(), true,
						"Feeling better", true,
						null));

		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.fatigue().value()).isEqualTo(3);
		assertThat(updated.motivation().value()).isEqualTo(4);
		assertThat(updated.discomfortAreas()).isEmpty();
		assertThat(updated.notes()).isEqualTo("Feeling better");

		List<DailyRecoveryCheckInRevisionResult> revisions =
				listDailyRecoveryCheckInRevisionsUseCase.execute(accountId, created.id());
		assertThat(revisions).hasSize(1);
		assertThat(revisions.getFirst().revisionNumber()).isEqualTo(1);
		assertThat(revisions.getFirst().priorFatigue().value()).isEqualTo(4);
		assertThat(revisions.getFirst().newFatigue().value()).isEqualTo(3);
		assertThat(revisions.getFirst().priorDiscomfort()).hasSize(1);
		assertThat(revisions.getFirst().newDiscomfort()).isEmpty();

		rebuildAthleteTrainingLoadUseCase.execute(accountId);

		DailyRecoveryCheckInResult afterRebuild = getDailyRecoveryCheckInUseCase.execute(accountId, created.id());
		assertThat(afterRebuild.fatigue().value()).isEqualTo(3);
		assertThat(afterRebuild.notes()).isEqualTo("Feeling better");
		assertThat(listDailyRecoveryCheckInRevisionsUseCase.execute(accountId, created.id())).hasSize(1);

		RecoveryCheckInCalendarDayResult loadAfterRebuild = getRecoveryCheckInCalendarUseCase.execute(
						accountId, TRAINING_DATE, TRAINING_DATE)
				.days()
				.getFirst();
		assertThat(loadAfterRebuild.trainingLoad().totalVolumeKilograms()).isEqualByComparingTo("2400");

		DailyRecoveryCheckInResult restDay = createDailyRecoveryCheckInUseCase.execute(
				accountId,
				REST_DATE,
				null,
				null,
				2,
				2,
				1,
				4,
				4,
				null,
				null);
		RecoveryCheckInCalendarDayResult restCalendar = getRecoveryCheckInCalendarUseCase.execute(
						accountId, REST_DATE, REST_DATE)
				.days()
				.getFirst();
		assertThat(restDay.checkInDate()).isEqualTo(REST_DATE);
		assertThat(restCalendar.trainingLoad().occurrenceCount()).isZero();
		assertThat(restCalendar.trainingLoad().totalSessionRpeLoad()).isEqualByComparingTo("0");

		AthleteRecoveryHistoryResult history = getAthleteRecoveryHistoryUseCase.execute(
				accountId, weekStart, weekEnd, true);
		assertThat(history.days()).hasSize(2);
	}

	private void completeMixedSession(AccountId accountId, Prescription prescription, LocalDate scheduledDate) {
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), scheduledDate, null, null);

		WorkoutExerciseExecutionId backSquatExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.BACK_SQUAT).id();
		WorkoutExerciseExecutionId frontSquatExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.FRONT_SQUAT).id();
		WorkoutExerciseExecutionId runningExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.RUNNING).id();
		WorkoutExerciseExecutionId plankExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.PLANK).id();

		substituteWorkoutExerciseExecutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id(),
				frontSquatExecutionId, SystemExerciseDefinitions.GOBLET_SQUAT,
				ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE, "No rack", null);

		completeStrengthExercise(accountId, prescription, occurrence.occurrence().id(), backSquatExecutionId,
				new BigDecimal("100"), 5, 3);
		completeStrengthExercise(accountId, prescription, occurrence.occurrence().id(), frontSquatExecutionId,
				new BigDecimal("30"), 10, 3);
		completeDistanceExercise(accountId, prescription, occurrence.occurrence().id(), runningExecutionId,
				new BigDecimal("5000"), 1800);
		completeDurationExercise(accountId, prescription, occurrence.occurrence().id(), plankExecutionId, 120);

		completeWorkoutOccurrenceUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id());

		submitWorkoutSessionEffortUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id(),
				new BigDecimal("8.0"), 60, "Hard but manageable");
		updateWorkoutSessionEffortUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id(),
				new BigDecimal("8.5"), 65, "Felt heavier on review");
	}

	private Prescription prescribeMixedSession(AccountId accountId) {
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Recovery Load Block", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Mixed", null, 1, DayOfWeek.FRIDAY, null, null, null);
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
		return new Prescription(accountId, plan.id(), day.id());
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
							null, false, reps, true, weight, true, WeightUnit.KILOGRAM, true,
							null, false, null, false, null, false, null, false, null, false, null, false));
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
						null, false, null, false, null, false, null, false,
						durationSeconds, true, distanceMeters, true, DistanceUnit.METER, true,
						null, false, null, false, null, false));
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
						null, false, null, false, null, false, null, false,
						durationSeconds, true, null, false, null, false,
						null, false, null, false, null, false));
		completeWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id());
		completeWorkoutExerciseExecutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Jordan", "Reed", LocalDate.of(1994, 3, 14), Sex.FEMALE,
				Height.ofCentimeters(168), Weight.ofKilograms(64),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		return accountId;
	}

	private record Prescription(AccountId accountId, TrainingPlanId planId, WorkoutDayId dayId) {
	}

}
