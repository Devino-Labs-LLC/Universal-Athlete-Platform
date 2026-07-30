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

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.SystemExerciseSubstitutionRelationships;
import com.devinolabs.uap.training.domain.TrainingEnvironmentArchivedException;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TrainingEnvironmentAcceptanceIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingEnvironmentUseCase createTrainingEnvironmentUseCase;

	@Autowired
	private SetDefaultTrainingEnvironmentUseCase setDefaultTrainingEnvironmentUseCase;

	@Autowired
	private UpdateTrainingEnvironmentUseCase updateTrainingEnvironmentUseCase;

	@Autowired
	private ArchiveTrainingEnvironmentUseCase archiveTrainingEnvironmentUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Autowired
	private SetWorkoutOccurrenceTrainingEnvironmentUseCase setWorkoutOccurrenceTrainingEnvironmentUseCase;

	@Autowired
	private ListOccurrenceExerciseSubstitutionCandidatesUseCase listOccurrenceExerciseSubstitutionCandidatesUseCase;

	@Autowired
	private SubstituteWorkoutExerciseExecutionUseCase substituteWorkoutExerciseExecutionUseCase;

	@Autowired
	private ListWorkoutExerciseSubstitutionHistoryUseCase listWorkoutExerciseSubstitutionHistoryUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private StartWorkoutExerciseSetUseCase startWorkoutExerciseSetUseCase;

	@Autowired
	private GetWorkoutOccurrenceUseCase getWorkoutOccurrenceUseCase;

	@Autowired
	private ListExerciseSubstitutionCandidatesUseCase listExerciseSubstitutionCandidatesUseCase;

	@Test
	void criticalAcceptanceScenarioCoversEnvironmentSnapshotsSubstitutionAndLocks() {
		AccountId accountId = athlete();
		TrainingEnvironmentResult homeGym = createTrainingEnvironmentUseCase.execute(
				accountId,
				"Home Gym",
				TrainingEnvironmentType.HOME_GYM,
				List.of(
						EquipmentType.DUMBBELL,
						EquipmentType.BENCH,
						EquipmentType.RESISTANCE_BAND,
						EquipmentType.PULL_UP_BAR,
						EquipmentType.OPEN_SPACE),
				null,
				null,
				false);
		TrainingEnvironmentResult commercialGym = createTrainingEnvironmentUseCase.execute(
				accountId,
				"Commercial Gym",
				TrainingEnvironmentType.COMMERCIAL_GYM,
				List.of(
						EquipmentType.BARBELL,
						EquipmentType.SQUAT_RACK,
						EquipmentType.BENCH,
						EquipmentType.DUMBBELL,
						EquipmentType.CABLE_MACHINE,
						EquipmentType.PLATE_LOADED_MACHINE,
						EquipmentType.TREADMILL,
						EquipmentType.COURT,
						EquipmentType.OPEN_SPACE),
				null,
				null,
				true);
		setDefaultTrainingEnvironmentUseCase.execute(accountId, commercialGym.id());

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId,
				TrainingPlanType.STRENGTH,
				null,
				"Strength Block",
				null,
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 8, 31),
				null,
				null,
				commercialGym.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId,
				plan.id(),
				"Lower",
				null,
				1,
				DayOfWeek.MONDAY,
				null,
				null,
				null);
		createWorkoutExerciseUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				SystemExerciseDefinitions.BACK_SQUAT,
				"Back Squat",
				ExerciseCategory.STRENGTH,
				ExerciseType.BARBELL,
				5,
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
				null);

		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				LocalDate.of(2026, 6, 8),
				null,
				null);
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		WorkoutExerciseExecutionId executionId = occurrence.executions().getFirst().id();

		assertThat(occurrence.occurrence().environment().plannedEnvironment().nameSnapshot())
				.isEqualTo("Commercial Gym");
		assertThat(occurrence.occurrence().environment().plannedEnvironment().trainingEnvironmentId())
				.isEqualTo(commercialGym.id());
		assertThat(occurrence.occurrence().environment().plannedEnvironment().availableEquipmentSnapshot())
				.contains(EquipmentType.BARBELL, EquipmentType.SQUAT_RACK);
		assertThat(occurrence.occurrence().environment().actualEnvironment().nameSnapshot())
				.isEqualTo("Commercial Gym");

		WorkoutOccurrenceResult switched = setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				homeGym.id());
		assertThat(switched.environment().plannedEnvironment().nameSnapshot()).isEqualTo("Commercial Gym");
		assertThat(switched.environment().actualEnvironment().nameSnapshot()).isEqualTo("Home Gym");
		assertThat(switched.environment().actualEnvironment().availableEquipmentSnapshot())
				.doesNotContain(EquipmentType.BARBELL, EquipmentType.SQUAT_RACK);

		List<OccurrenceSubstitutionCandidateResult> candidates =
				listOccurrenceExerciseSubstitutionCandidatesUseCase.execute(
						accountId,
						plan.id(),
						day.id(),
						occurrenceId,
						executionId);
		assertThat(candidates).extracting(OccurrenceSubstitutionCandidateResult::targetExerciseDefinitionId)
				.contains(SystemExerciseDefinitions.GOBLET_SQUAT)
				.doesNotContain(SystemExerciseDefinitions.LEG_PRESS);
		assertThat(candidates.getFirst().environmentContext().nameSnapshot()).isEqualTo("Home Gym");

		substituteWorkoutExerciseExecutionUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				executionId,
				SystemExerciseDefinitions.GOBLET_SQUAT,
				ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE,
				"Rack taken",
				SystemExerciseSubstitutionRelationships.BACK_SQUAT_TO_GOBLET_SQUAT);
		WorkoutExerciseSubstitutionResult history = listWorkoutExerciseSubstitutionHistoryUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				executionId).getFirst();
		assertThat(history.trainingEnvironmentNameSnapshot()).isEqualTo("Home Gym");
		assertThat(history.availableEquipmentSnapshot()).contains(EquipmentType.DUMBBELL);
		assertThat(history.availableEquipmentSnapshot()).doesNotContain(EquipmentType.BARBELL);

		startWorkoutExerciseSetUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				executionId,
				listWorkoutExerciseSetsUseCase.execute(
						accountId, plan.id(), day.id(), occurrenceId, executionId).getFirst().id());
		assertThatThrownBy(() -> setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, commercialGym.id()))
				.isInstanceOf(WorkoutOccurrenceEnvironmentLockedException.class);

		updateTrainingEnvironmentUseCase.execute(
				accountId,
				homeGym.id(),
				new UpdateTrainingEnvironmentCommand(
						"Garage Gym", true,
						null, false,
						List.of(EquipmentType.DUMBBELL, EquipmentType.BARBELL), true,
						null, false,
						null, false,
						null, false));

		WorkoutOccurrenceDetailResult reloaded = getWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId);
		assertThat(reloaded.occurrence().environment().actualEnvironment().nameSnapshot()).isEqualTo("Home Gym");
		assertThat(reloaded.occurrence().environment().actualEnvironment().availableEquipmentSnapshot())
				.doesNotContain(EquipmentType.BARBELL);
		assertThat(listWorkoutExerciseSubstitutionHistoryUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, executionId).getFirst()
				.trainingEnvironmentNameSnapshot()).isEqualTo("Home Gym");

		WorkoutOccurrenceDetailResult nextOccurrence = createWorkoutOccurrenceUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				LocalDate.of(2026, 6, 15),
				null,
				null);
		assertThat(nextOccurrence.occurrence().environment().plannedEnvironment().nameSnapshot())
				.isEqualTo("Commercial Gym");

		archiveTrainingEnvironmentUseCase.execute(accountId, commercialGym.id());
		assertThat(reloaded.occurrence().environment().plannedEnvironment().nameSnapshot())
				.isEqualTo("Commercial Gym");
		assertThatThrownBy(() -> setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				nextOccurrence.occurrence().id(),
				commercialGym.id()))
				.isInstanceOf(TrainingEnvironmentArchivedException.class);

		assertThatThrownBy(() -> listExerciseSubstitutionCandidatesUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BACK_SQUAT,
				List.of(EquipmentType.DUMBBELL),
				commercialGym.id()))
				.isInstanceOf(ConflictingEquipmentContextFiltersException.class);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Alex",
				"Rivera",
				LocalDate.of(1998, 3, 14),
				Sex.MALE,
				Height.ofCentimeters(183),
				Weight.ofKilograms(84),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		return accountId;
	}

}
