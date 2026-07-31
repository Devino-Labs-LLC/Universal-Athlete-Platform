package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.CreateTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.CreateTrainingPlanUseCase;
import com.devinolabs.uap.training.application.CreateWorkoutDayUseCase;
import com.devinolabs.uap.training.application.CreateWorkoutExerciseUseCase;
import com.devinolabs.uap.training.application.CreateWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.WorkoutOccurrenceDetailResult;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WorkoutFeasibilityHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

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
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Test
	void feasibilityEndpointsRequireAuthAndReturn200ForIncompatibleEquipment() throws Exception {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Jamie",
				"Fox",
				LocalDate.of(1992, 1, 10),
				Sex.FEMALE,
				Height.ofCentimeters(170),
				Weight.ofKilograms(65),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);

		var homeGym = createTrainingEnvironmentUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				"Home Gym",
				TrainingEnvironmentType.HOME_GYM,
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH),
				null,
				null,
				true);
		var plan = createTrainingPlanUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				TrainingPlanType.STRENGTH,
				null,
				"HTTP Plan",
				null,
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 8, 31),
				null,
				null,
				homeGym.id().value());
		var day = createWorkoutDayUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				plan.id(),
				"Day A",
				null,
				1,
				DayOfWeek.MONDAY,
				null,
				null,
				null);
		createWorkoutExerciseUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				plan.id(),
				day.id(),
				SystemExerciseDefinitions.BACK_SQUAT,
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

		String dayFeasibilityUrl = "/api/v1/training/plans/" + plan.id().value() + "/days/" + day.id().value()
				+ "/feasibility?trainingEnvironmentId=" + homeGym.id().value();

		mockMvc.perform(get(dayFeasibilityUrl))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(get(dayFeasibilityUrl).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.summary.status").value("NOT_FEASIBLE"))
				.andExpect(jsonPath("$.summary.feasibilityPercentage").value(0))
				.andExpect(jsonPath("$.exercises", hasSize(1)))
				.andExpect(jsonPath("$.exercises[0].feasible").value(false));

		mockMvc.perform(get("/api/v1/training/plans/" + plan.id().value() + "/feasibility?usePreferredEnvironments=true")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("NOT_FEASIBLE"));

		mockMvc.perform(get("/api/v1/training/plans/" + plan.id().value() + "/feasibility")
						.with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_FEASIBILITY_ENVIRONMENT_MODE"));

		mockMvc.perform(get(dayFeasibilityUrl + "&suggestionLimit=11").with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_FEASIBILITY_SUGGESTION_LIMIT"));

		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				plan.id(),
				day.id(),
				LocalDate.of(2026, 6, 8),
				null,
				null);
		String occurrenceId = occurrence.occurrence().id().value().toString();
		mockMvc.perform(get("/api/v1/training/plans/" + plan.id().value() + "/days/" + day.id().value()
						+ "/occurrences/" + occurrenceId + "/feasibility?suggestionLimit=0")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.executions[0].suggestedSubstitutions", hasSize(0)));
	}

	private static RequestPostProcessor accountAuth(AccountId accountId) {
		AccountPrincipal principal = new AccountPrincipal(accountId);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.authorities());
		return authentication(authentication);
	}

}
