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
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutFeasibilityRankingIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingEnvironmentUseCase createTrainingEnvironmentUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private AnalyzeWorkoutDayFeasibilityUseCase analyzeWorkoutDayFeasibilityUseCase;

	@Test
	void suggestionLimitAndRankingBehaveDeterministically() {
		AccountId accountId = athlete();
		var environment = createTrainingEnvironmentUseCase.execute(
				accountId,
				"Hotel Gym",
				TrainingEnvironmentType.HOME_GYM,
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH),
				null,
				null,
				true);
		var plan = createTrainingPlanUseCase.execute(
				accountId,
				TrainingPlanType.STRENGTH,
				null,
				"Ranking Plan",
				null,
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 8, 31),
				null,
				null,
				environment.id().value());
		var day = createWorkoutDayUseCase.execute(
				accountId,
				plan.id(),
				"Lower",
				null,
				1,
				DayOfWeek.MONDAY,
				null,
				null,
				null);
		prescribe(accountId, plan.id(), day.id(), SystemExerciseDefinitions.BACK_SQUAT);

		ExerciseFeasibilityAnalysisResult exercise = analyzeWorkoutDayFeasibilityUseCase.execute(
				accountId, plan.id(), day.id(), environment.id(), 3, false).exercises().getFirst();
		assertThat(exercise.suggestedSubstitutions()).hasSize(1);
		assertThat(exercise.suggestedSubstitutions().getFirst().rankingPosition()).isEqualTo(1);
		assertThat(exercise.suggestedSubstitutions().getFirst().compatibilityLevel())
				.isEqualTo(ExerciseSubstitutionCompatibility.HIGH);
		assertThat(exercise.suggestedSubstitutions().getFirst().targetExerciseDefinitionId())
				.isEqualTo(SystemExerciseDefinitions.GOBLET_SQUAT);

		assertThat(analyzeWorkoutDayFeasibilityUseCase.execute(
				accountId, plan.id(), day.id(), environment.id(), 0, false)
				.exercises().getFirst().suggestedSubstitutions()).isEmpty();

		assertThatThrownBy(() -> analyzeWorkoutDayFeasibilityUseCase.execute(
				accountId, plan.id(), day.id(), environment.id(), 11, false))
				.isInstanceOf(InvalidFeasibilitySuggestionLimitException.class);
	}

	private void prescribe(AccountId accountId, com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId, ExerciseDefinitionId definitionId) {
		createWorkoutExerciseUseCase.execute(
				accountId,
				planId,
				dayId,
				definitionId,
				"Back Squat",
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
				null);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Casey",
				"Ng",
				LocalDate.of(1990, 4, 4),
				Sex.MALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(78),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		return accountId;
	}

}
