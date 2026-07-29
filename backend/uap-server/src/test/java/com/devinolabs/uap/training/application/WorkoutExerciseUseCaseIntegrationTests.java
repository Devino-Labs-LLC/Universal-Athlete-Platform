package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingPlanStatusAction;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseStatusAction;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutExerciseUseCaseIntegrationTests {

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
	private CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;

	@Autowired
	private ListWorkoutExercisesUseCase listWorkoutExercisesUseCase;

	@Autowired
	private GetWorkoutExerciseUseCase getWorkoutExerciseUseCase;

	@Autowired
	private UpdateWorkoutExerciseUseCase updateWorkoutExerciseUseCase;

	@Autowired
	private ReorderWorkoutExercisesUseCase reorderWorkoutExercisesUseCase;

	@Autowired
	private ChangeWorkoutExerciseStatusUseCase changeWorkoutExerciseStatusUseCase;

	@Autowired
	private DeleteWorkoutExerciseUseCase deleteWorkoutExerciseUseCase;

	@Test
	void createsListsOrdersUpdatesDeletesAndLifecycle() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower Body", null, 1, DayOfWeek.MONDAY, null, 60, null);

		WorkoutExerciseResult squat = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BACK_SQUAT,
				"  Back   Squat  ", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				4, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, 120, 8, "3-0-1", "Brace hard", null);
		assertThat(squat.displayOrder()).isZero();
		assertThat(squat.exerciseName()).isEqualTo("Back   Squat");
		assertThat(squat.exerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(squat.status()).isEqualTo(WorkoutExerciseStatus.PLANNED);

		WorkoutExerciseResult rdl = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.ROMANIAN_DEADLIFT,
				"Romanian Deadlift", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 8, 10, null, null, null, null, null, 90, null, null, null, null);
		assertThat(rdl.displayOrder()).isEqualTo(1);

		WorkoutExerciseResult inserted = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), customDefinition(accountId, "Leg Press"),
				"Leg Press", ExerciseCategory.STRENGTH, ExerciseType.MACHINE,
				3, 10, 12, null, null, null, null, null, null, null, null, null, 1);
		assertThat(inserted.displayOrder()).isEqualTo(1);

		List<WorkoutExerciseResult> listed = listWorkoutExercisesUseCase.execute(accountId, plan.id(), day.id());
		assertThat(listed).extracting(WorkoutExerciseResult::exerciseName)
				.containsExactly("Back   Squat", "Leg Press", "Romanian Deadlift");
		assertThat(listed).extracting(WorkoutExerciseResult::displayOrder).containsExactly(0, 1, 2);

		assertThatThrownBy(() -> createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BACK_SQUAT,
				"back squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, null, null, null, null, null, null, null, null, null, null))
				.isInstanceOf(DuplicateWorkoutExerciseException.class);

		List<WorkoutExerciseResult> reordered = reorderWorkoutExercisesUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				List.of(rdl.id().value(), squat.id().value(), inserted.id().value()));
		assertThat(reordered).extracting(r -> r.id().value())
				.containsExactly(rdl.id().value(), squat.id().value(), inserted.id().value());
		assertThat(reordered).extracting(WorkoutExerciseResult::displayOrder).containsExactly(0, 1, 2);

		WorkoutExerciseResult updated = updateWorkoutExerciseUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				squat.id(),
				new UpdateWorkoutExerciseCommand(
						null, false,
						"Back Squat Heavy", true,
						null, false,
						null, false,
						5, true,
						3, true,
						5, true,
						new BigDecimal("110"), true,
						WeightUnit.KILOGRAM, true,
						null, false,
						null, false,
						null, false,
						150, true,
						9, true,
						null, true,
						null, true,
						0, true));
		assertThat(updated.exerciseName()).isEqualTo("Back Squat Heavy");
		assertThat(updated.sets()).isEqualTo(5);
		assertThat(updated.minimumReps()).isEqualTo(3);
		assertThat(updated.maximumReps()).isEqualTo(5);
		assertThat(updated.targetWeight()).isEqualByComparingTo("110");
		assertThat(updated.targetRestSeconds()).isEqualTo(150);
		assertThat(updated.targetRpe()).isEqualTo(9);
		assertThat(updated.tempo()).isNull();
		assertThat(updated.coachingNotes()).isNull();
		assertThat(updated.displayOrder()).isZero();

		changeWorkoutExerciseStatusUseCase.execute(
				accountId, plan.id(), day.id(), rdl.id(), WorkoutExerciseStatusAction.ACTIVATE);
		assertThatThrownBy(() -> deleteWorkoutExerciseUseCase.execute(accountId, plan.id(), day.id(), rdl.id()))
				.isInstanceOf(WorkoutExerciseDeleteNotAllowedException.class);

		changeWorkoutExerciseStatusUseCase.execute(
				accountId, plan.id(), day.id(), rdl.id(), WorkoutExerciseStatusAction.COMPLETE);
		assertThat(getWorkoutExerciseUseCase.execute(accountId, plan.id(), day.id(), rdl.id()).status())
				.isEqualTo(WorkoutExerciseStatus.COMPLETED);

		deleteWorkoutExerciseUseCase.execute(accountId, plan.id(), day.id(), inserted.id());
		List<WorkoutExerciseResult> afterDelete = listWorkoutExercisesUseCase.execute(accountId, plan.id(), day.id());
		assertThat(afterDelete).extracting(WorkoutExerciseResult::displayOrder).containsExactly(0, 1);
	}

	@Test
	void rejectsArchivedPlanInvalidStatusAndCrossAccount() {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();
		createAthlete(owner);
		createAthlete(other);

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				owner, TrainingPlanType.GENERAL, null, "General", null,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				owner, plan.id(), "Skills", null, 1, DayOfWeek.SATURDAY, null, null, null);
		WorkoutExerciseResult exercise = createWorkoutExerciseUseCase.execute(
				owner, plan.id(), day.id(), customDefinition(owner, "Layup Drill"),
				"Layup Drill", ExerciseCategory.SPORT_SKILL, ExerciseType.SPORT,
				3, 10, 10, null, null, 60, null, null, null, null, null, null, null);

		assertThatThrownBy(() -> changeWorkoutExerciseStatusUseCase.execute(
				owner, plan.id(), day.id(), exercise.id(), WorkoutExerciseStatusAction.COMPLETE))
				.isInstanceOf(InvalidWorkoutExerciseStatusException.class);

		assertThatThrownBy(() -> listWorkoutExercisesUseCase.execute(other, plan.id(), day.id()))
				.isInstanceOf(TrainingPlanNotFoundException.class);
		assertThatThrownBy(() -> getWorkoutExerciseUseCase.execute(other, plan.id(), day.id(), exercise.id()))
				.isInstanceOf(TrainingPlanNotFoundException.class);

		changeTrainingPlanStatusUseCase.execute(owner, plan.id(), TrainingPlanStatusAction.ARCHIVE);
		assertThatThrownBy(() -> createWorkoutExerciseUseCase.execute(
				owner, plan.id(), day.id(), customDefinition(owner, "New Drill"),
				"New Drill", ExerciseCategory.SPORT_SKILL, ExerciseType.SPORT,
				2, 8, 8, null, null, null, null, null, null, null, null, null, null))
				.isInstanceOf(TrainingPlanArchivedException.class);

		assertThatThrownBy(() -> reorderWorkoutExercisesUseCase.execute(
				owner, plan.id(), day.id(), List.of(exercise.id().value(), UUID.randomUUID())))
				.isInstanceOf(TrainingPlanArchivedException.class);
	}

	private ExerciseDefinitionId customDefinition(AccountId accountId, String canonicalName) {
		return createAthleteExerciseDefinitionUseCase.execute(accountId, canonicalName, ExerciseDefinitionMetadataFixtures.defaultCustom()).id();
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
