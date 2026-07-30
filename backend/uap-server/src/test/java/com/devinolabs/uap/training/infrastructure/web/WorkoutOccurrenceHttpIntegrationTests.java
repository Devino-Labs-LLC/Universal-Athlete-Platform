package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WorkoutOccurrenceHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullOccurrenceLifecycleAuthCsrfFiltersAndDetailExecutions() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		String exerciseId = createExercise(accountId, planId, dayId);
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences";

		mockMvc.perform(get(base))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"2026-07-28"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult created = mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "scheduledDate":"2026-07-28",
								  "plannedStartTime":"08:30:00",
								  "athleteNotes":"Focus day"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("SCHEDULED"))
				.andExpect(jsonPath("$.executions", hasSize(1)))
				.andExpect(jsonPath("$.executions[0].prescribedSets").value(4))
				.andExpect(jsonPath("$.athleteId").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist())
				.andReturn();
		String occurrenceId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		String executionId = JsonPath.read(created.getResponse().getContentAsString(), "$.executions[0].id");

		mockMvc.perform(get(base + "?status=SCHEDULED&scheduledFrom=2026-07-01&scheduledTo=2026-07-31")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(occurrenceId));

		mockMvc.perform(get(base + "/" + occurrenceId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.executions[0].sourceWorkoutExerciseId").value(exerciseId));

		mockMvc.perform(patch(base + "/" + occurrenceId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"athleteNotes":null,"plannedStartTime":"09:00:00"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.plannedStartTime").value("09:00:00"))
				.andExpect(jsonPath("$.athleteNotes").value(nullValue()));

		mockMvc.perform(post(base + "/" + occurrenceId + "/start")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.startedAt").isNotEmpty());

		String exercisesBase = base + "/" + occurrenceId + "/exercises";
		mockMvc.perform(post(exercisesBase + "/" + executionId + "/start")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		mockMvc.perform(patch(exercisesBase + "/" + executionId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"actualSets":4,"actualReps":5,"actualWeight":100,"weightUnit":"KILOGRAM"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("WORKOUT_EXERCISE_EXECUTION_ACTUALS_ARE_SET_DERIVED"));

		String setsBase = exercisesBase + "/" + executionId + "/sets";
		List<String> setIds = JsonPath.read(
				mockMvc.perform(get(setsBase).with(accountAuth(accountId)))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$", hasSize(4)))
						.andReturn().getResponse().getContentAsString(),
				"$[*].id");
		for (String setId : setIds) {
			mockMvc.perform(patch(setsBase + "/" + setId)
							.with(accountAuth(accountId))
							.with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"actualReps":5,"actualWeight":100,"actualWeightUnit":"KILOGRAM"}
									"""))
					.andExpect(status().isOk());
			mockMvc.perform(post(setsBase + "/" + setId + "/complete")
							.with(accountAuth(accountId))
							.with(csrf()))
					.andExpect(status().isOk());
		}

		mockMvc.perform(post(exercisesBase + "/" + executionId + "/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.actualSets").value(4))
				.andExpect(jsonPath("$.actualReps").value(20))
				.andExpect(jsonPath("$.actualWeight").value(100));

		mockMvc.perform(post(base + "/" + occurrenceId + "/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.completedAt").isNotEmpty());

		mockMvc.perform(delete(base + "/" + occurrenceId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_OCCURRENCE_DELETE_NOT_ALLOWED"));

		mockMvc.perform(post(base + "/" + occurrenceId + "/skip")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_OCCURRENCE_STATUS"));
	}

	@Test
	void cancelAndTerminalParentExecutionMutationsUseStableErrorCodes() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		createExercise(accountId, planId, dayId);
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences";

		MvcResult created = mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"2026-08-05"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String occurrenceId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		String executionId = JsonPath.read(created.getResponse().getContentAsString(), "$.executions[0].id");
		String exercisesBase = base + "/" + occurrenceId + "/exercises";

		mockMvc.perform(post(exercisesBase + "/" + executionId + "/start")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		mockMvc.perform(get(base + "/" + occurrenceId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		mockMvc.perform(post(base + "/" + occurrenceId + "/cancel")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_OCCURRENCE_STATUS"));

		MvcResult cancelledCreate = mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"2026-08-06"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String cancelledId = JsonPath.read(cancelledCreate.getResponse().getContentAsString(), "$.id");
		String cancelledExecutionId = JsonPath.read(
				cancelledCreate.getResponse().getContentAsString(), "$.executions[0].id");

		mockMvc.perform(post(base + "/" + cancelledId + "/cancel")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"))
				.andExpect(jsonPath("$.completedAt").value(nullValue()))
				.andExpect(jsonPath("$.executions[0].status").value("NOT_STARTED"))
				.andExpect(jsonPath("$.executions[0].startedAt").value(nullValue()))
				.andExpect(jsonPath("$.executions[0].completedAt").value(nullValue()));

		mockMvc.perform(post(base + "/" + cancelledId + "/exercises/" + cancelledExecutionId + "/start")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_OCCURRENCE_STATUS"));
	}

	@Test
	void occurrenceEnvironmentAndSubstitutionCandidatesAreAuthenticatedAndMapped() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String commercialGymId = createEnvironment(
				accountId,
				"Commercial Gym",
				"COMMERCIAL_GYM",
				"[\"BARBELL\",\"SQUAT_RACK\",\"PLATE_LOADED_MACHINE\"]",
				true);
		String homeGymId = createEnvironment(
				accountId,
				"Home Gym",
				"HOME_GYM",
				"[\"DUMBBELL\",\"BENCH\",\"RESISTANCE_BAND\"]",
				false);
		String planId = createPlan(accountId, commercialGymId);
		String dayId = createDay(accountId, planId);
		String exerciseId = createExercise(accountId, planId, dayId);
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences";

		MvcResult created = mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"2026-07-28"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.environment.plannedEnvironment.nameSnapshot").value("Commercial Gym"))
				.andExpect(jsonPath("$.environment.actualEnvironment.nameSnapshot").value("Commercial Gym"))
				.andReturn();
		String occurrenceId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		String executionId = JsonPath.read(created.getResponse().getContentAsString(), "$.executions[0].id");
		String environmentPath = base + "/" + occurrenceId + "/environment";
		String candidatesPath = base + "/" + occurrenceId + "/exercises/" + executionId + "/substitution-candidates";

		mockMvc.perform(put(environmentPath)
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"trainingEnvironmentId":"%s"}
								""".formatted(homeGymId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(put(environmentPath)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"trainingEnvironmentId":"%s"}
								""".formatted(homeGymId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.environment.plannedEnvironment.nameSnapshot").value("Commercial Gym"))
				.andExpect(jsonPath("$.environment.actualEnvironment.nameSnapshot").value("Home Gym"));

		mockMvc.perform(get(candidatesPath).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].targetExerciseDefinitionId")
						.value(org.hamcrest.Matchers.hasItem(SystemExerciseDefinitions.GOBLET_SQUAT.value().toString())))
				.andExpect(jsonPath("$[*].targetExerciseDefinitionId")
						.value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(
								SystemExerciseDefinitions.LEG_PRESS.value().toString()))))
				.andExpect(jsonPath("$[0].environmentContext.nameSnapshot").value("Home Gym"));

		mockMvc.perform(delete(environmentPath)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.environment.plannedEnvironment.nameSnapshot").value("Commercial Gym"))
				.andExpect(jsonPath("$.environment.actualEnvironment").value(nullValue()));

		mockMvc.perform(delete(environmentPath)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_OCCURRENCE_ENVIRONMENT_NOT_SET"));

		mockMvc.perform(get(base + "/" + occurrenceId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.executions[0].sourceWorkoutExerciseId").value(exerciseId));
	}

	@Test
	void rejectsEmptyDayDuplicateDateAndCrossAccount() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String emptyDayId = createDay(accountId, planId, "Rest", "TUESDAY");
		String baseEmpty = "/api/v1/training/plans/" + planId + "/days/" + emptyDayId + "/occurrences";

		mockMvc.perform(post(baseEmpty)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"2026-07-28"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_OCCURRENCE_REQUIRES_EXERCISES"));

		String dayId = createDay(accountId, planId, "Upper", "WEDNESDAY");
		createExercise(accountId, planId, dayId);
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences";

		mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"2026-07-29"}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"2026-07-29"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_WORKOUT_OCCURRENCE"));

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get(base).with(accountAuth(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));
	}

	private void createProfile(AccountId accountId) throws Exception {
		mockMvc.perform(post("/api/v1/athletes/me")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName":"Jordan",
								  "lastName":"Lee",
								  "dateOfBirth":"1998-05-12",
								  "sex":"FEMALE",
								  "heightCm":175.00,
								  "weightKg":68.00,
								  "dominantHand":"RIGHT",
								  "dominantFoot":"RIGHT"
								}
								"""))
				.andExpect(status().isCreated());
	}

	private String createPlan(AccountId accountId) throws Exception {
		return createPlan(accountId, null);
	}

	private String createPlan(AccountId accountId, String defaultTrainingEnvironmentId) throws Exception {
		String defaultEnvironmentField = defaultTrainingEnvironmentId == null
				? ""
				: ",\"defaultTrainingEnvironmentId\":\"" + defaultTrainingEnvironmentId + "\"";
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "name":"Strength Plan",
								  "startDate":"2026-06-01",
								  "endDate":"2026-08-31"%s
								}
								""".formatted(defaultEnvironmentField)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createEnvironment(
			AccountId accountId,
			String name,
			String type,
			String equipmentJson,
			boolean defaultEnvironment) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/environments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"%s",
								  "type":"%s",
								  "availableEquipment":%s,
								  "defaultEnvironment":%s
								}
								""".formatted(name, type, equipmentJson, defaultEnvironment)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createDay(AccountId accountId, String planId) throws Exception {
		return createDay(accountId, planId, "Lower Body", "MONDAY");
	}

	private String createDay(AccountId accountId, String planId, String title, String scheduledDay) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"%s","planWeekNumber":1,"scheduledDayOfWeek":"%s"}
								""".formatted(title, scheduledDay)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createExercise(AccountId accountId, String planId, String dayId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseDefinitionId":"%s",
								  "exerciseName":"Back Squat",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":4,
								  "minimumReps":5,
								  "maximumReps":5
								}
								""".formatted(SystemExerciseDefinitions.BACK_SQUAT)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
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
