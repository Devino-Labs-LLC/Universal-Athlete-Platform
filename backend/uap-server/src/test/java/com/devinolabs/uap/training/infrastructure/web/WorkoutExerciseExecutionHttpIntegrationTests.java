package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WorkoutExerciseExecutionHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void executionLifecycleAuthCsrfAndValidation() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		createExercise(accountId, planId, dayId);
		String occurrenceId = createOccurrence(accountId, planId, dayId);
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId
				+ "/occurrences/" + occurrenceId + "/exercises";

		mockMvc.perform(get(base))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(get(base).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].status").value("NOT_STARTED"))
				.andExpect(jsonPath("$[0].prescribedSets").value(3))
				.andExpect(jsonPath("$[0].version").doesNotExist());

		String executionId = JsonPath.read(
				mockMvc.perform(get(base).with(accountAuth(accountId))).andReturn().getResponse().getContentAsString(),
				"$[0].id");

		mockMvc.perform(post(base + "/" + executionId + "/start")
						.with(accountAuth(accountId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(post(base + "/" + executionId + "/start")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.startedAt").isNotEmpty());

		mockMvc.perform(get(base + "/" + executionId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(executionId));

		mockMvc.perform(patch(base + "/" + executionId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"actualSets":3,"actualReps":8,"actualRpe":7,"athleteNotes":"Smooth"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("WORKOUT_EXERCISE_EXECUTION_ACTUALS_ARE_SET_DERIVED"));

		mockMvc.perform(patch(base + "/" + executionId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"athleteNotes":"Smooth"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.athleteNotes").value("Smooth"));

		mockMvc.perform(post(base + "/" + executionId + "/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_EXERCISE_EXECUTION_HAS_INCOMPLETE_SETS"));

		String setsBase = base + "/" + executionId + "/sets";
		List<String> setIds = JsonPath.read(
				mockMvc.perform(get(setsBase).with(accountAuth(accountId)))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$", hasSize(3)))
						.andReturn().getResponse().getContentAsString(),
				"$[*].id");
		for (String setId : setIds) {
			mockMvc.perform(patch(setsBase + "/" + setId)
							.with(accountAuth(accountId))
							.with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"actualReps":8,"actualRestSeconds":90,"actualRpe":7.5}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("IN_PROGRESS"));
			mockMvc.perform(post(setsBase + "/" + setId + "/complete")
							.with(accountAuth(accountId))
							.with(csrf()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("COMPLETED"));
		}

		mockMvc.perform(post(base + "/" + executionId + "/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.completedAt").isNotEmpty())
				.andExpect(jsonPath("$.actualSets").value(3))
				.andExpect(jsonPath("$.actualReps").value(24))
				.andExpect(jsonPath("$.actualRestSeconds").value(90))
				.andExpect(jsonPath("$.actualRpe").value(7.5))
				.andExpect(jsonPath("$.completedSetCount").value(3))
				.andExpect(jsonPath("$.setCount").value(3));

		mockMvc.perform(post(base + "/" + executionId + "/start")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_EXERCISE_EXECUTION_STATUS"));
	}

	@Test
	void skipExecutionAndCrossAccountAccess() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		createExercise(accountId, planId, dayId);
		String occurrenceId = createOccurrence(accountId, planId, dayId);
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId
				+ "/occurrences/" + occurrenceId + "/exercises";
		String executionId = JsonPath.read(
				mockMvc.perform(get(base).with(accountAuth(accountId))).andReturn().getResponse().getContentAsString(),
				"$[0].id");

		mockMvc.perform(post(base + "/" + executionId + "/skip")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SKIPPED"))
				.andExpect(jsonPath("$.completedAt").doesNotExist());

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get(base + "/" + executionId).with(accountAuth(other)))
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
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "name":"Strength Plan",
								  "startDate":"2026-06-01",
								  "endDate":"2026-08-31"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createDay(AccountId accountId, String planId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Lower Body","planWeekNumber":1,"scheduledDayOfWeek":"MONDAY"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private void createExercise(AccountId accountId, String planId, String dayId) throws Exception {
		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseName":"Romanian Deadlift",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":3,
								  "minimumReps":8,
								  "maximumReps":10
								}
								"""))
				.andExpect(status().isCreated());
	}

	private String createOccurrence(AccountId accountId, String planId, String dayId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"2026-07-28"}
								"""))
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
