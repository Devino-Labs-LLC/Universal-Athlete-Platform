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

import com.devinolabs.uap.ExerciseDefinitionMetadataFixtures;
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
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseFeasibilityStatus;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.FeasibilityEnvironmentContextSource;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.SystemExerciseSubstitutionRelationships;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutFeasibilityAcceptanceIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingEnvironmentUseCase createTrainingEnvironmentUseCase;

	@Autowired
	private CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;

	@Autowired
	private CreateExerciseSubstitutionRelationshipUseCase createExerciseSubstitutionRelationshipUseCase;

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
	private SubstituteWorkoutExerciseExecutionUseCase substituteWorkoutExerciseExecutionUseCase;

	@Autowired
	private UpdateTrainingEnvironmentUseCase updateTrainingEnvironmentUseCase;

	@Autowired
	private AnalyzeWorkoutDayFeasibilityUseCase analyzeWorkoutDayFeasibilityUseCase;

	@Autowired
	private AnalyzeWorkoutOccurrenceFeasibilityUseCase analyzeWorkoutOccurrenceFeasibilityUseCase;

	@Autowired
	private AnalyzeTrainingPlanFeasibilityUseCase analyzeTrainingPlanFeasibilityUseCase;

	@Test
	void criticalAcceptanceScenarioCoversDayOccurrenceAndSnapshotFidelity() {
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
				true);
		createTrainingEnvironmentUseCase.execute(
				accountId,
				"Commercial Gym",
				TrainingEnvironmentType.COMMERCIAL_GYM,
				List.of(
						EquipmentType.BARBELL,
						EquipmentType.SQUAT_RACK,
						EquipmentType.BENCH,
						EquipmentType.DUMBBELL,
						EquipmentType.CABLE_MACHINE),
				null,
				null,
				false);

		ExerciseDefinitionId pullUpId = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Pull-Up", ExerciseDefinitionMetadataFixtures.pullUp()).id();
		ExerciseDefinitionId dumbbellBenchPressId = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Dumbbell Bench Press", ExerciseDefinitionMetadataFixtures.dumbbellBenchPress()).id();
		var benchToDbRelationship = createExerciseSubstitutionRelationshipUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BENCH_PRESS,
				dumbbellBenchPressId,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				ExerciseSubstitutionCompatibility.HIGH,
				"Dumbbells at home");

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId,
				TrainingPlanType.STRENGTH,
				null,
				"Feasibility Block",
				null,
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 8, 31),
				null,
				null,
				homeGym.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId,
				plan.id(),
				"Full Body",
				null,
				1,
				DayOfWeek.MONDAY,
				null,
				null,
				null);
		prescribe(accountId, plan.id(), day.id(), SystemExerciseDefinitions.BACK_SQUAT, "Back Squat", ExerciseType.BARBELL);
		prescribe(accountId, plan.id(), day.id(), SystemExerciseDefinitions.BENCH_PRESS, "Bench Press", ExerciseType.BARBELL);
		prescribe(accountId, plan.id(), day.id(), pullUpId, "Pull-Up", ExerciseType.BODYWEIGHT);
		prescribe(accountId, plan.id(), day.id(), SystemExerciseDefinitions.PLANK, "Plank", ExerciseType.BODYWEIGHT);

		WorkoutDayFeasibilityResult dayFeasibility = analyzeWorkoutDayFeasibilityUseCase.execute(
				accountId, plan.id(), day.id(), homeGym.id(), 3, false);
		assertThat(dayFeasibility.summary().status()).isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);
		assertThat(dayFeasibility.summary().feasibilityPercentage()).isEqualByComparingTo(new BigDecimal("50.00"));
		assertThat(dayFeasibility.summary().feasibleExercises()).isEqualTo(2);
		assertThat(dayFeasibility.summary().totalExercises()).isEqualTo(4);
		assertThat(dayFeasibility.exercises()).hasSize(4);
		assertThat(dayFeasibility.exercises().get(0).currentStatus())
				.isEqualTo(ExerciseFeasibilityStatus.MISSING_REQUIRED_EQUIPMENT);
		assertThat(dayFeasibility.exercises().get(0).hasCompatibleSubstitution()).isTrue();
		assertThat(dayFeasibility.exercises().get(0).suggestedSubstitutions()).extracting(
				result -> result.targetExerciseDefinitionId())
				.contains(SystemExerciseDefinitions.GOBLET_SQUAT);
		assertThat(dayFeasibility.exercises().get(1).hasCompatibleSubstitution()).isTrue();
		assertThat(dayFeasibility.exercises().get(1).suggestedSubstitutions()).extracting(
				result -> result.targetExerciseDefinitionId())
				.contains(dumbbellBenchPressId);
		assertThat(dayFeasibility.exercises().get(2).feasible()).isTrue();
		assertThat(dayFeasibility.exercises().get(3).feasible()).isTrue();

		TrainingPlanFeasibilityResult planFeasibility = analyzeTrainingPlanFeasibilityUseCase.execute(
				accountId, plan.id(), homeGym.id(), false, 3, false);
		assertThat(planFeasibility.feasibilityPercentage()).isEqualByComparingTo(new BigDecimal("50.00"));
		assertThat(planFeasibility.status()).isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);

		TrainingPlanFeasibilityResult preferredPlanFeasibility = analyzeTrainingPlanFeasibilityUseCase.execute(
				accountId, plan.id(), null, true, 3, false);
		assertThat(preferredPlanFeasibility.daySummaries().getFirst().environmentContext().contextSource())
				.isEqualTo(FeasibilityEnvironmentContextSource.PLAN_DEFAULT);

		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), LocalDate.of(2026, 6, 8), null, null);
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, homeGym.id());

		List<WorkoutExerciseExecutionResult> executions = occurrence.executions();
		WorkoutExerciseExecutionId backSquatExecution = executions.get(0).id();
		WorkoutExerciseExecutionId benchPressExecution = executions.get(1).id();

		substituteWorkoutExerciseExecutionUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				backSquatExecution,
				SystemExerciseDefinitions.GOBLET_SQUAT,
				ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE,
				"No rack",
				SystemExerciseSubstitutionRelationships.BACK_SQUAT_TO_GOBLET_SQUAT);
		substituteWorkoutExerciseExecutionUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				benchPressExecution,
				dumbbellBenchPressId,
				ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE,
				"No barbell",
				benchToDbRelationship.id());

		WorkoutOccurrenceFeasibilityResult occurrenceFeasibility = analyzeWorkoutOccurrenceFeasibilityUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, 3, false);
		assertThat(occurrenceFeasibility.summary().status()).isEqualTo(WorkoutFeasibilityStatus.FULLY_FEASIBLE);
		assertThat(occurrenceFeasibility.summary().feasibilityPercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
		assertThat(occurrenceFeasibility.environmentContext().contextSource())
				.isEqualTo(FeasibilityEnvironmentContextSource.OCCURRENCE_ACTUAL_SNAPSHOT);
		assertThat(occurrenceFeasibility.executions().get(0).currentStatus())
				.isEqualTo(ExerciseFeasibilityStatus.FEASIBLE_AS_PERFORMED);
		assertThat(occurrenceFeasibility.executions().get(1).currentStatus())
				.isEqualTo(ExerciseFeasibilityStatus.FEASIBLE_AS_PERFORMED);

		updateTrainingEnvironmentUseCase.execute(
				accountId,
				homeGym.id(),
				new UpdateTrainingEnvironmentCommand(
						"Garage Gym", true,
						null, false,
						List.of(EquipmentType.DUMBBELL, EquipmentType.BARBELL, EquipmentType.BENCH), true,
						null, false,
						null, false,
						null, false));

		WorkoutOccurrenceFeasibilityResult snapshotFeasibility = analyzeWorkoutOccurrenceFeasibilityUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, 3, false);
		assertThat(snapshotFeasibility.environmentContext().trainingEnvironmentName()).isEqualTo("Home Gym");
		assertThat(snapshotFeasibility.environmentContext().availableEquipment()).doesNotContain(EquipmentType.BARBELL);
		assertThat(snapshotFeasibility.summary().status()).isEqualTo(WorkoutFeasibilityStatus.FULLY_FEASIBLE);
	}

	private void prescribe(
			AccountId accountId,
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			ExerciseDefinitionId definitionId,
			String name,
			ExerciseType type) {
		createWorkoutExerciseUseCase.execute(
				accountId,
				planId,
				dayId,
				definitionId,
				name,
				ExerciseCategory.STRENGTH,
				type,
				3,
				8,
				12,
				new BigDecimal("40"),
				WeightUnit.KILOGRAM,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Morgan",
				"Lee",
				LocalDate.of(1995, 8, 2),
				Sex.MALE,
				Height.ofCentimeters(180),
				Weight.ofKilograms(82),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		return accountId;
	}

}
