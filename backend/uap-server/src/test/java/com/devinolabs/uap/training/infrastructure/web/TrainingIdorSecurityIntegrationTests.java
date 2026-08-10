package com.devinolabs.uap.training.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.ExerciseDefinitionHttpPayloads;
import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.jayway.jsonpath.JsonPath;

/**
 * Focused ownership / IDOR hardening: foreign athlete IDs and nested path mismatches must 404
 * without confirming that the underlying resource exists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrainingIdorSecurityIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Test
	void foreignAthleteIdsAndNestedMismatchesReturnNotFoundWithoutExistenceLeakage() throws Exception {
		AccountId owner = athlete();
		AccountId attacker = athlete();

		OwnedGraph graph = seedOwnedGraph(owner);
		String attackerPlanId = createPlan(attacker, "Attacker Plan");
		String attackerDayId = createDay(attacker, attackerPlanId, "Attacker Day", "WEDNESDAY");

		mockMvc.perform(get("/api/v1/training/plans/" + graph.planId()).with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/plans/" + graph.planId() + "/days/" + graph.dayId())
						.with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/plans/" + graph.planId()
						+ "/days/" + graph.dayId()
						+ "/exercises/" + graph.exerciseId()).with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/plans/" + graph.planId()
						+ "/days/" + graph.dayId()
						+ "/occurrences/" + graph.occurrenceId()).with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/plans/" + graph.planId()
						+ "/days/" + graph.dayId()
						+ "/occurrences/" + graph.occurrenceId()
						+ "/exercises/" + graph.executionId()).with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/plans/" + graph.planId()
						+ "/days/" + graph.dayId()
						+ "/occurrences/" + graph.occurrenceId()
						+ "/exercises/" + graph.executionId()
						+ "/sets/" + graph.setId()).with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));

		// Nested mismatch: attacker's plan + owner's day/occurrence chain.
		mockMvc.perform(get("/api/v1/training/plans/" + attackerPlanId
						+ "/days/" + graph.dayId()
						+ "/occurrences/" + graph.occurrenceId()).with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKOUT_DAY_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/plans/" + graph.planId()
						+ "/days/" + attackerDayId
						+ "/occurrences/" + graph.occurrenceId()).with(auth(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKOUT_DAY_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/plans/" + graph.planId()
						+ "/days/" + graph.dayId()
						+ "/occurrences/" + UUID.randomUUID()).with(auth(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKOUT_OCCURRENCE_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/environments/" + graph.environmentId()).with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_ENVIRONMENT_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/exercise-definitions/" + graph.customDefinitionId())
						.with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EXERCISE_DEFINITION_NOT_ACCESSIBLE"));

		mockMvc.perform(get("/api/v1/training/recovery-check-ins/" + graph.checkInId()).with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECOVERY_CHECK_IN_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/adaptation-proposals/" + graph.proposalId()).with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKOUT_ADAPTATION_PROPOSAL_NOT_FOUND"));

		mockMvc.perform(patch("/api/v1/training/adaptation-proposals/" + graph.proposalId()
						+ "/items/" + graph.proposalItemId())
						.with(auth(attacker))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"decision":"REJECTED","athleteNotes":"probe"}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKOUT_ADAPTATION_PROPOSAL_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/training/client/plans/" + graph.planId()
						+ "/days/" + graph.dayId()
						+ "/occurrences/" + graph.occurrenceId()
						+ "/launch-context").with(auth(attacker)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/training/athlete-state/snapshots/" + UUID.randomUUID())
						.with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DAILY_ATHLETE_STATE_SNAPSHOT_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Daily athlete state snapshot was not found"));

		mockMvc.perform(get("/api/v1/training/readiness/assessments/" + UUID.randomUUID())
						.with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DAILY_READINESS_ASSESSMENT_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Daily readiness assessment was not found"));

		mockMvc.perform(get("/api/v1/training/recommendations/" + UUID.randomUUID())
						.with(auth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DAILY_TRAINING_RECOMMENDATION_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Daily training recommendation was not found"));
	}

	private OwnedGraph seedOwnedGraph(AccountId owner) throws Exception {
		String environmentId = createEnvironment(owner);
		String customDefinitionId = createCustomDefinition(owner);
		String planId = createPlan(owner, "Owner Plan");
		String dayId = createDay(owner, planId, "Owner Day", "MONDAY");
		String exerciseId = createExercise(owner, planId, dayId);
		String occurrenceId = createOccurrence(owner, planId, dayId, "2026-07-20");
		String executionId = firstExecutionId(owner, planId, dayId, occurrenceId);
		String setId = firstSetId(owner, planId, dayId, occurrenceId, executionId);
		String checkInId = createCheckIn(owner, "2026-07-20");

		mockMvc.perform(put("/api/v1/training/plans/" + planId
						+ "/days/" + dayId
						+ "/occurrences/" + occurrenceId
						+ "/environment")
						.with(auth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"trainingEnvironmentId":"%s"}
								""".formatted(environmentId)))
				.andExpect(status().isOk());

		MvcResult proposal = mockMvc.perform(post("/api/v1/training/plans/" + planId
						+ "/days/" + dayId
						+ "/occurrences/" + occurrenceId
						+ "/adaptation-proposals")
						.with(auth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"suggestionLimit":3,"includeAlternatives":false,"expirationMinutes":30}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String proposalBody = proposal.getResponse().getContentAsString();
		String proposalId = JsonPath.read(proposalBody, "$.id");
		String proposalItemId = JsonPath.read(proposalBody, "$.items[0].id");

		return new OwnedGraph(
				planId,
				dayId,
				exerciseId,
				occurrenceId,
				executionId,
				setId,
				environmentId,
				customDefinitionId,
				checkInId,
				proposalId,
				proposalItemId);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Pat",
				"Lee",
				LocalDate.of(1996, 4, 12),
				Sex.FEMALE,
				Height.ofCentimeters(170),
				Weight.ofKilograms(65),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		return accountId;
	}

	private String createPlan(AccountId accountId, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "name":"%s",
								  "startDate":"2026-06-01",
								  "endDate":"2026-08-31"
								}
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createDay(AccountId accountId, String planId, String title, String dayOfWeek) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"%s","planWeekNumber":1,"scheduledDayOfWeek":"%s"}
								""".formatted(title, dayOfWeek)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createExercise(AccountId accountId, String planId, String dayId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseDefinitionId":"%s",
								  "exerciseName":"Back Squat",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":3,
								  "minimumReps":5,
								  "maximumReps":5
								}
								""".formatted(SystemExerciseDefinitions.BACK_SQUAT)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createOccurrence(AccountId accountId, String planId, String dayId, String date) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"%s"}
								""".formatted(date)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String firstExecutionId(AccountId accountId, String planId, String dayId, String occurrenceId)
			throws Exception {
		String body = mockMvc.perform(get("/api/v1/training/plans/" + planId + "/days/" + dayId
						+ "/occurrences/" + occurrenceId + "/exercises").with(auth(accountId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(body, "$[0].id");
	}

	private String firstSetId(
			AccountId accountId,
			String planId,
			String dayId,
			String occurrenceId,
			String executionId) throws Exception {
		String body = mockMvc.perform(get("/api/v1/training/plans/" + planId + "/days/" + dayId
						+ "/occurrences/" + occurrenceId
						+ "/exercises/" + executionId
						+ "/sets").with(auth(accountId)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return JsonPath.read(body, "$[0].id");
	}

	private String createEnvironment(AccountId accountId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/environments")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Owner Home",
								  "type":"HOME_GYM",
								  "availableEquipment":["DUMBBELL","BENCH"],
								  "defaultEnvironment":true
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createCustomDefinition(AccountId accountId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/exercise-definitions")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(ExerciseDefinitionHttpPayloads.createPayload("Owner Custom Press")))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createCheckIn(AccountId accountId, String date) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/recovery-check-ins")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "checkInDate":"%s",
								  "sleepDurationMinutes":420,
								  "sleepQuality":3,
								  "fatigue":3,
								  "muscleSoreness":3,
								  "stress":2,
								  "mood":4,
								  "motivation":3
								}
								""".formatted(date)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static RequestPostProcessor auth(AccountId accountId) {
		AccountPrincipal principal = new AccountPrincipal(accountId);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.authorities());
		return authentication(authentication);
	}

	private record OwnedGraph(
			String planId,
			String dayId,
			String exerciseId,
			String occurrenceId,
			String executionId,
			String setId,
			String environmentId,
			String customDefinitionId,
			String checkInId,
			String proposalId,
			String proposalItemId) {
	}

}
