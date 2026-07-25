package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

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
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.TrainingPlanStatusAction;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutSessionStatus;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutSessionUseCaseIntegrationTests {

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
	private StartWorkoutExerciseUseCase startWorkoutExerciseUseCase;

	@Autowired
	private GetWorkoutSessionUseCase getWorkoutSessionUseCase;

	@Autowired
	private UpdateWorkoutSessionUseCase updateWorkoutSessionUseCase;

	@Autowired
	private CompleteWorkoutExerciseUseCase completeWorkoutExerciseUseCase;

	@Autowired
	private SkipWorkoutExerciseUseCase skipWorkoutExerciseUseCase;

	@Test
	void startsUpdatesCompletesAndSkipsSessions() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower Body", null, DayOfWeek.MONDAY, null, 60, null);
		WorkoutExerciseResult squat = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(),
				"Back Squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				4, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, 120, 8, null, null, null);
		WorkoutExerciseResult rdl = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(),
				"Romanian Deadlift", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 8, 10, null, null, null, null, null, 90, null, null, null, null);

		assertThatThrownBy(() -> getWorkoutSessionUseCase.execute(accountId, plan.id(), day.id(), squat.id()))
				.isInstanceOf(WorkoutSessionNotFoundException.class);

		WorkoutSessionResult started = startWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), squat.id());
		assertThat(started.status()).isEqualTo(WorkoutSessionStatus.IN_PROGRESS);
		assertThat(started.completedAt()).isNull();

		WorkoutSessionResult startedAgain = startWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), squat.id());
		assertThat(startedAgain.id()).isEqualTo(started.id());
		assertThat(startedAgain.status()).isEqualTo(WorkoutSessionStatus.IN_PROGRESS);

		WorkoutSessionResult updated = updateWorkoutSessionUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				squat.id(),
				new UpdateWorkoutSessionCommand(
						4, true,
						5, true,
						new BigDecimal("102.5"), true,
						WeightUnit.KILOGRAM, true,
						null, false,
						null, false,
						null, false,
						90, true,
						8, true,
						"  Felt strong  ", true));
		assertThat(updated.actualSets()).isEqualTo(4);
		assertThat(updated.actualReps()).isEqualTo(5);
		assertThat(updated.actualWeight()).isEqualByComparingTo("102.5");
		assertThat(updated.athleteNotes()).isEqualTo("Felt strong");

		WorkoutSessionResult completed = completeWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), squat.id());
		assertThat(completed.status()).isEqualTo(WorkoutSessionStatus.COMPLETED);
		assertThat(completed.completedAt()).isNotNull();
		assertThat(getWorkoutSessionUseCase.execute(accountId, plan.id(), day.id(), squat.id()).status())
				.isEqualTo(WorkoutSessionStatus.COMPLETED);

		assertThatThrownBy(() -> startWorkoutExerciseUseCase.execute(accountId, plan.id(), day.id(), squat.id()))
				.isInstanceOf(InvalidWorkoutSessionStatusException.class);

		WorkoutSessionResult skipped = skipWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), rdl.id());
		assertThat(skipped.status()).isEqualTo(WorkoutSessionStatus.SKIPPED);
		assertThat(skipped.completedAt()).isNull();
	}

	@Test
	void rejectsArchivedPlanInvalidCompleteAndCrossAccount() {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();
		createAthlete(owner);
		createAthlete(other);

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				owner, TrainingPlanType.GENERAL, null, "General", null,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				owner, plan.id(), "Skills", null, DayOfWeek.SATURDAY, null, null, null);
		WorkoutExerciseResult exercise = createWorkoutExerciseUseCase.execute(
				owner, plan.id(), day.id(),
				"Layup Drill", ExerciseCategory.SPORT_SKILL, ExerciseType.SPORT,
				3, 10, 10, null, null, 60, null, null, null, null, null, null, null);

		assertThatThrownBy(() -> completeWorkoutExerciseUseCase.execute(
				owner, plan.id(), day.id(), exercise.id()))
				.isInstanceOf(WorkoutSessionNotFoundException.class);

		startWorkoutExerciseUseCase.execute(owner, plan.id(), day.id(), exercise.id());

		assertThatThrownBy(() -> getWorkoutSessionUseCase.execute(other, plan.id(), day.id(), exercise.id()))
				.isInstanceOf(TrainingPlanNotFoundException.class);

		changeTrainingPlanStatusUseCase.execute(owner, plan.id(), TrainingPlanStatusAction.ARCHIVE);
		assertThatThrownBy(() -> startWorkoutExerciseUseCase.execute(owner, plan.id(), day.id(), exercise.id()))
				.isInstanceOf(TrainingPlanArchivedException.class);
		assertThatThrownBy(() -> completeWorkoutExerciseUseCase.execute(owner, plan.id(), day.id(), exercise.id()))
				.isInstanceOf(TrainingPlanArchivedException.class);
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
