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

import com.devinolabs.uap.ExerciseDefinitionFixtures;
import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanStatusAction;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutOccurrenceUseCaseIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private ChangeTrainingPlanStatusUseCase changeTrainingPlanStatusUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private ExerciseDefinitionFixtures exerciseDefinitions;

	@Autowired
	private UpdateWorkoutExerciseUseCase updateWorkoutExerciseUseCase;

	@Autowired
	private DeleteWorkoutExerciseUseCase deleteWorkoutExerciseUseCase;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Autowired
	private ListWorkoutOccurrencesUseCase listWorkoutOccurrencesUseCase;

	@Autowired
	private GetWorkoutOccurrenceUseCase getWorkoutOccurrenceUseCase;

	@Autowired
	private StartWorkoutOccurrenceUseCase startWorkoutOccurrenceUseCase;

	@Autowired
	private CompleteWorkoutOccurrenceUseCase completeWorkoutOccurrenceUseCase;

	@Autowired
	private SkipWorkoutExerciseExecutionUseCase skipWorkoutExerciseExecutionUseCase;

	@Autowired
	private StartWorkoutExerciseExecutionUseCase startWorkoutExerciseExecutionUseCase;

	@Autowired
	private UpdateWorkoutExerciseExecutionUseCase updateWorkoutExerciseExecutionUseCase;

	@Autowired
	private CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;

	@Autowired
	private DeleteWorkoutOccurrenceUseCase deleteWorkoutOccurrenceUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private UpdateWorkoutExerciseSetUseCase updateWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseSetUseCase completeWorkoutExerciseSetUseCase;

	@Test
	void preservesHistoricalSnapshotWhenSourceExerciseChanges() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower Body", null, 1, DayOfWeek.MONDAY, null, 60, null);
		WorkoutExerciseResult squat = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), exerciseDefinitions.idFor(accountId, "Back Squat"),
				"Back Squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				5, 5, 5, new BigDecimal("225"), WeightUnit.POUND,
				null, null, null, 120, 8, null, null, null);

		LocalDate scheduledDate = LocalDate.of(2026, 8, 3);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), scheduledDate, null, null);
		WorkoutExerciseExecutionResult execution = occurrence.executions().getFirst();
		assertThat(execution.prescribedSets()).isEqualTo(5);
		assertThat(execution.prescribedTargetWeight()).isEqualByComparingTo("225");

		updateWorkoutExerciseUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				squat.id(),
				new UpdateWorkoutExerciseCommand(
						null, false,
						null, false,
						null, false,
						null, false,
						4, true,
						8, true,
						8, true,
						new BigDecimal("185"), true,
						WeightUnit.POUND, true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false));

		startWorkoutOccurrenceUseCase.execute(accountId, plan.id(), day.id(), occurrence.occurrence().id());
		startWorkoutExerciseExecutionUseCase.execute(
				accountId, plan.id(), day.id(), occurrence.occurrence().id(), execution.id());
		assertThatThrownBy(() -> updateWorkoutExerciseExecutionUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrence.occurrence().id(),
				execution.id(),
				new UpdateWorkoutExerciseExecutionCommand(
						5, true,
						5, true,
						new BigDecimal("225"), true,
						WeightUnit.POUND, true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false)))
				.isInstanceOf(WorkoutExerciseExecutionActualsAreSetDerivedException.class);
		logAndCompleteSets(
				accountId,
				plan.id(),
				day.id(),
				occurrence.occurrence().id(),
				execution.id(),
				1,
				new BigDecimal("225"));
		completeWorkoutExerciseExecutionUseCase.execute(
				accountId, plan.id(), day.id(), occurrence.occurrence().id(), execution.id());
		WorkoutOccurrenceDetailResult completed = completeWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), occurrence.occurrence().id());

		WorkoutExerciseExecutionResult historical = completed.executions().getFirst();
		assertThat(historical.prescribedSets()).isEqualTo(5);
		assertThat(historical.prescribedMinimumReps()).isEqualTo(5);
		assertThat(historical.prescribedMaximumReps()).isEqualTo(5);
		assertThat(historical.prescribedTargetWeight()).isEqualByComparingTo("225");
		assertThat(historical.actualSets()).isEqualTo(5);
		assertThat(historical.actualReps()).isEqualTo(5);
		assertThat(historical.actualWeight()).isEqualByComparingTo("225");
		assertThat(historical.weightUnit()).isEqualTo(WeightUnit.POUND);
		assertThat(completed.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.COMPLETED);
	}

	@Test
	void rejectsEmptyExercisesDuplicateDateArchivedPlanDeleteSourceAndIncompleteChildren() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), null, null);
		WorkoutDayResult emptyDay = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Rest", null, 1, DayOfWeek.TUESDAY, null, null, null);
		LocalDate date = LocalDate.of(2026, 7, 29);

		assertThatThrownBy(() -> createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), emptyDay.id(), date, null, null))
				.isInstanceOf(WorkoutOccurrenceRequiresExercisesException.class);

		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Upper", null, 1, DayOfWeek.WEDNESDAY, null, null, null);
		WorkoutExerciseResult bench = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), exerciseDefinitions.idFor(accountId, "Bench Press"),
				"Bench Press", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 8, 8, new BigDecimal("135"), WeightUnit.POUND,
				null, null, null, null, null, null, null, null);
		WorkoutExerciseResult row = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), exerciseDefinitions.idFor(accountId, "Barbell Row"),
				"Barbell Row", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 10, 10, null, null, null, null, null, null, null, null, null, null);

		WorkoutOccurrenceDetailResult first = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), date, null, null);
		assertThatThrownBy(() -> createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), date, null, null))
				.isInstanceOf(DuplicateWorkoutOccurrenceException.class);

		deleteWorkoutExerciseUseCase.execute(accountId, plan.id(), day.id(), row.id());
		WorkoutOccurrenceDetailResult afterDelete = getWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), first.occurrence().id());
		assertThat(afterDelete.executions()).hasSize(2);
		assertThat(afterDelete.executions()).extracting(e -> e.sourceWorkoutExerciseId().value())
				.contains(bench.id().value(), row.id().value());

		startWorkoutOccurrenceUseCase.execute(accountId, plan.id(), day.id(), first.occurrence().id());
		assertThatThrownBy(() -> completeWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), first.occurrence().id()))
				.isInstanceOf(WorkoutOccurrenceHasIncompleteExercisesException.class);

		for (WorkoutExerciseExecutionResult execution : afterDelete.executions()) {
			if (execution.id().equals(afterDelete.executions().getFirst().id())) {
				startWorkoutExerciseExecutionUseCase.execute(
						accountId, plan.id(), day.id(), first.occurrence().id(), execution.id());
				logAndCompleteSets(
						accountId, plan.id(), day.id(), first.occurrence().id(), execution.id(), 8, null);
				completeWorkoutExerciseExecutionUseCase.execute(
						accountId, plan.id(), day.id(), first.occurrence().id(), execution.id());
			}
			else {
				skipWorkoutExerciseExecutionUseCase.execute(
						accountId, plan.id(), day.id(), first.occurrence().id(), execution.id());
			}
		}
		completeWorkoutOccurrenceUseCase.execute(accountId, plan.id(), day.id(), first.occurrence().id());

		TrainingPlanResult archivedPlan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.GENERAL, null, "Archived Plan", null,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), null, null);
		WorkoutDayResult archivedDay = createWorkoutDayUseCase.execute(
				accountId, archivedPlan.id(), "Day", null, 1, DayOfWeek.FRIDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, archivedPlan.id(), archivedDay.id(), exerciseDefinitions.idFor(accountId, "Push Up"),
				"Push Up", ExerciseCategory.STRENGTH, ExerciseType.BODYWEIGHT,
				3, 10, 10, null, null, null, null, null, null, null, null, null, null);
		changeTrainingPlanStatusUseCase.execute(accountId, archivedPlan.id(), TrainingPlanStatusAction.ARCHIVE);
		assertThatThrownBy(() -> createWorkoutOccurrenceUseCase.execute(
				accountId, archivedPlan.id(), archivedDay.id(), LocalDate.of(2026, 7, 30), null, null))
				.isInstanceOf(TrainingPlanArchivedException.class);

		WorkoutDayResult deletableDay = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Temp", null, 1, DayOfWeek.THURSDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), deletableDay.id(), exerciseDefinitions.idFor(accountId, "Curl"),
				"Curl", ExerciseCategory.STRENGTH, ExerciseType.DUMBBELL,
				2, 12, 12, null, null, null, null, null, null, null, null, null, null);
		WorkoutOccurrenceDetailResult scheduled = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), deletableDay.id(), LocalDate.of(2026, 8, 1), null, null);
		deleteWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), deletableDay.id(), scheduled.occurrence().id());
		assertThat(listWorkoutOccurrencesUseCase.execute(
				accountId, plan.id(), deletableDay.id(), null, null, null)).isEmpty();
	}

	private void logAndCompleteSets(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			Integer reps,
			BigDecimal weight) {
		for (WorkoutExerciseSetResult set : listWorkoutExerciseSetsUseCase.execute(
				accountId, planId, dayId, occurrenceId, executionId)) {
			updateWorkoutExerciseSetUseCase.execute(
					accountId,
					planId,
					dayId,
					occurrenceId,
					executionId,
					set.id(),
					new UpdateWorkoutExerciseSetCommand(
							null, false,
							reps, true,
							weight, weight != null,
							weight == null ? null : WeightUnit.POUND, weight != null,
							null, false,
							null, false,
							null, false,
							null, false,
							null, false,
							null, false));
			completeWorkoutExerciseSetUseCase.execute(
					accountId, planId, dayId, occurrenceId, executionId, set.id());
		}
	}

	private void createAthlete(AccountId accountId) {
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Jordan",
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
	}

}
