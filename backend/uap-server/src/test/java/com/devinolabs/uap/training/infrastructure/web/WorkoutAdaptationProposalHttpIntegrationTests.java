package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
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
import com.devinolabs.uap.training.application.SetWorkoutOccurrenceTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.WorkoutOccurrenceDetailResult;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WorkoutAdaptationProposalHttpIntegrationTests {

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

	@Autowired
	private SetWorkoutOccurrenceTrainingEnvironmentUseCase setWorkoutOccurrenceTrainingEnvironmentUseCase;

	@Test
	void adaptationProposalEndpointsRequireAuthAndSupportLifecycle() throws Exception {
		Session session = session();
		String generateUrl = "/api/v1/training/plans/%s/days/%s/occurrences/%s/adaptation-proposals"
				.formatted(session.planId(), session.dayId(), session.occurrenceId());

		mockMvc.perform(post(generateUrl).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());

		String proposalJson = mockMvc.perform(post(generateUrl)
						.with(auth(session.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "suggestionLimit": 3,
								  "includeAlternatives": false,
								  "expirationMinutes": 30
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("READY"))
				.andExpect(jsonPath("$.totalExecutions").value(2))
				.andExpect(jsonPath("$.expectedFeasibleExecutions").value(2))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String proposalId = proposalJson.split("\"id\":\"")[1].split("\"")[0];
		long version = Long.parseLong(proposalJson.split("\"version\":")[1].split("[,}]")[0]);

		mockMvc.perform(get("/api/v1/training/adaptation-proposals/" + proposalId).with(auth(session.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(proposalId));

		mockMvc.perform(get("/api/v1/training/adaptation-proposals")
						.param("occurrenceId", session.occurrenceId())
						.with(auth(session.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		String itemId = proposalJson.split("\"items\":\\[\\{\"id\":\"")[1].split("\"")[0];
		String acceptedJson = mockMvc.perform(patch("/api/v1/training/adaptation-proposals/" + proposalId + "/items/" + itemId)
						.with(auth(session.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"decision":"ACCEPTED"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].athleteDecision").value("ACCEPTED"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		long updatedVersion = Long.parseLong(acceptedJson.split("\"version\":")[1].split("[,}]")[0]);

		String applyUrl = "/api/v1/training/plans/%s/days/%s/occurrences/%s/adaptation-proposals/%s/apply"
				.formatted(session.planId(), session.dayId(), session.occurrenceId(), proposalId);
		mockMvc.perform(post(applyUrl)
						.with(auth(session.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"expectedProposalVersion": %d}
								""".formatted(version)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_ADAPTATION_PROPOSAL_VERSION_CONFLICT"));

		mockMvc.perform(post(applyUrl)
						.with(auth(session.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"expectedProposalVersion": %d}
								""".formatted(updatedVersion)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.proposalStatus").value("APPLIED"));

		mockMvc.perform(post("/api/v1/training/adaptation-proposals/" + proposalId + "/cancel")
						.with(auth(session.accountId()))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_ADAPTATION_PROPOSAL_TERMINAL"));
	}

	private Session session() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Jamie",
				"Fox",
				LocalDate.of(1992, 1, 10),
				Sex.FEMALE,
				Height.ofCentimeters(168),
				Weight.ofKilograms(62),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		var homeGym = createTrainingEnvironmentUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				"Home Gym",
				TrainingEnvironmentType.HOME_GYM,
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH, EquipmentType.OPEN_SPACE),
				null,
				null,
				true);
		var plan = createTrainingPlanUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				TrainingPlanType.STRENGTH,
				null,
				"HTTP Adaptation",
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
		createWorkoutExerciseUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				plan.id(),
				day.id(),
				SystemExerciseDefinitions.PLANK,
				"Plank",
				ExerciseCategory.STRENGTH,
				ExerciseType.BODYWEIGHT,
				3,
				30,
				60,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				plan.id(),
				day.id(),
				LocalDate.of(2026, 6, 8),
				null,
				null);
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value()),
				plan.id(),
				day.id(),
				occurrenceId,
				homeGym.id());
		return new Session(accountId, plan.id().value().toString(), day.id().value().toString(), occurrenceId.value().toString());
	}

	private static RequestPostProcessor auth(AccountId accountId) {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				new AccountPrincipal(accountId),
				null,
				List.of());
		return authentication(authentication);
	}

	private record Session(AccountId accountId, String planId, String dayId, String occurrenceId) {
	}

}
