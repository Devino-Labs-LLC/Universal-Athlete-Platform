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

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrainingPerformanceHttpIntegrationTests {

	private static final String PERFORMANCE = "/api/v1/training/performance";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void occurrencePerformanceIsAuthenticatedAndRollsUpTheSession() throws Exception {
		AccountId accountId = AccountId.generate();
		Workout workout = loggedSession(accountId, "2026-07-06");

		mockMvc.perform(get(workout.occurrenceBase() + "/performance"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(get(workout.occurrenceBase() + "/performance").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.occurrenceId").value(workout.occurrenceId()))
				.andExpect(jsonPath("$.scheduledDate").value("2026-07-06"))
				.andExpect(jsonPath("$.totals.completedExerciseCount").value(1))
				.andExpect(jsonPath("$.totals.completedSetCount").value(2))
				.andExpect(jsonPath("$.totals.totalRepetitions").value(9))
				.andExpect(jsonPath("$.exercises", hasSize(1)))
				.andExpect(jsonPath("$.exercises[0].exerciseName").value("Back Squat"))
				.andExpect(jsonPath("$.exercises[0].exercisePerformanceKey")
						.value(workout.exercisePerformanceKey()))
				.andExpect(jsonPath("$.exercises[0].metrics.heaviestWeight.normalizedUnit").value("KILOGRAM"))
				.andExpect(jsonPath("$.exercises[0].metrics.bestEstimatedOneRepMax.estimated").value(true))
				.andExpect(jsonPath("$.exercises[0].metrics.heaviestWeight.estimated").value(false));

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get(workout.occurrenceBase() + "/performance").with(accountAuth(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));
	}

	@Test
	void exerciseHistoryAndPersonalRecordsAreExposedByPerformanceKey() throws Exception {
		AccountId accountId = AccountId.generate();
		Workout workout = loggedSession(accountId, "2026-07-13");
		String key = workout.exercisePerformanceKey();

		mockMvc.perform(get(PERFORMANCE + "/exercises/" + key).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exercisePerformanceKey").value(key))
				.andExpect(jsonPath("$.exerciseName").value("Back Squat"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.entries", hasSize(1)))
				.andExpect(jsonPath("$.entries[0].scheduledDate").value("2026-07-13"))
				.andExpect(jsonPath("$.entries[0].metrics.completedSetCount").value(2));

		mockMvc.perform(get(PERFORMANCE + "/exercises/" + key + "/personal-records")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(6)));

		mockMvc.perform(get(PERFORMANCE + "/personal-records")
						.param("exercisePerformanceKey", key)
						.param("recordType", "HEAVIEST_WEIGHT")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].recordType").value("HEAVIEST_WEIGHT"))
				.andExpect(jsonPath("$[0].measuredUnit").value("KILOGRAM"))
				.andExpect(jsonPath("$[0].estimated").value(false))
				.andExpect(jsonPath("$[0].exercisePerformanceKey").value(key));

		mockMvc.perform(get(PERFORMANCE + "/personal-records/recent")
						.param("days", "30")
						.param("limit", "3")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	void unknownKeysAndOutOfRangeParametersAreRejected() throws Exception {
		AccountId accountId = AccountId.generate();
		Workout workout = loggedSession(accountId, "2026-07-20");

		mockMvc.perform(get(PERFORMANCE + "/exercises/" + UUID.randomUUID()).with(accountAuth(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EXERCISE_PERFORMANCE_KEY_NOT_FOUND"));

		mockMvc.perform(get(PERFORMANCE + "/exercises/" + workout.exercisePerformanceKey())
						.param("size", "101")
						.with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAINING_PERFORMANCE_RANGE"));

		mockMvc.perform(get(PERFORMANCE + "/exercises/" + workout.exercisePerformanceKey())
						.param("scheduledFrom", "2026-07-30")
						.param("scheduledTo", "2026-07-01")
						.with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAINING_PERFORMANCE_RANGE"));

		mockMvc.perform(get(PERFORMANCE + "/personal-records/recent")
						.param("days", "400")
						.with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAINING_PERFORMANCE_RANGE"));
	}

	@Test
	void recomputeRequiresCsrfAndACompletedExecution() throws Exception {
		AccountId accountId = AccountId.generate();
		Workout workout = loggedSession(accountId, "2026-07-27");
		String recompute = workout.occurrenceBase() + "/exercises/" + workout.executionId()
				+ "/performance/recompute";

		mockMvc.perform(post(recompute).with(accountAuth(accountId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(post(recompute).with(accountAuth(accountId)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.executionId").value(workout.executionId()))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.metrics.completedSetCount").value(2));

		Workout untouched = loggedSession(accountId, "2026-08-03", false);
		mockMvc.perform(post(untouched.occurrenceBase() + "/exercises/" + untouched.executionId()
						+ "/performance/recompute")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRAINING_METRICS_REQUIRE_COMPLETED_EXECUTION"));
	}

	private Workout loggedSession(AccountId accountId, String scheduledDate) throws Exception {
		createProfile(accountId);
		return loggedSession(accountId, scheduledDate, true);
	}

	private Workout loggedSession(AccountId accountId, String scheduledDate, boolean complete) throws Exception {
		String planId = planId(accountId, scheduledDate);
		String dayId = dayId(accountId, planId);
		String exercisePerformanceKey = createExercise(accountId, planId, dayId);
		String occurrenceId = createOccurrence(accountId, planId, dayId, scheduledDate);
		String occurrenceBase = "/api/v1/training/plans/" + planId + "/days/" + dayId
				+ "/occurrences/" + occurrenceId;
		String executionId = firstExecutionId(accountId, occurrenceBase);
		String setsBase = occurrenceBase + "/exercises/" + executionId + "/sets";

		List<String> setIds = JsonPath.read(
				mockMvc.perform(get(setsBase).with(accountAuth(accountId)))
						.andExpect(status().isOk())
						.andReturn().getResponse().getContentAsString(),
				"$[*].id");
		logSet(accountId, setsBase, setIds.get(0), 5, "100");
		logSet(accountId, setsBase, setIds.get(1), 4, "105");
		if (complete) {
			mockMvc.perform(post(occurrenceBase + "/exercises/" + executionId + "/complete")
							.with(accountAuth(accountId))
							.with(csrf()))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status").value("COMPLETED"));
		}
		return new Workout(occurrenceBase, occurrenceId, executionId, exercisePerformanceKey);
	}

	private void logSet(AccountId accountId, String setsBase, String setId, int reps, String weight)
			throws Exception {
		mockMvc.perform(patch(setsBase + "/" + setId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"actualReps\":" + reps + ",\"actualWeight\":" + weight
								+ ",\"actualWeightUnit\":\"KILOGRAM\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post(setsBase + "/" + setId + "/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));
	}

	private String firstExecutionId(AccountId accountId, String occurrenceBase) throws Exception {
		return JsonPath.read(
				mockMvc.perform(get(occurrenceBase + "/exercises").with(accountAuth(accountId)))
						.andExpect(status().isOk())
						.andReturn().getResponse().getContentAsString(),
				"$[0].id");
	}

	private void createProfile(AccountId accountId) throws Exception {
		mockMvc.perform(post("/api/v1/athletes/me")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName":"Avery",
								  "lastName":"Nkemdi",
								  "dateOfBirth":"1995-11-02",
								  "sex":"FEMALE",
								  "heightCm":170.00,
								  "weightKg":66.00,
								  "dominantHand":"RIGHT",
								  "dominantFoot":"RIGHT"
								}
								"""))
				.andExpect(status().isCreated());
	}

	private String planId(AccountId accountId, String label) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "name":"Performance Plan %s",
								  "startDate":"2026-06-01",
								  "endDate":"2026-12-31"
								}
								""".formatted(label)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String dayId(AccountId accountId, String planId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Lower","planWeekNumber":1,"scheduledDayOfWeek":"MONDAY"}
								"""))
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
								  "sets":2,
								  "minimumReps":4,
								  "maximumReps":6
								}
								""".formatted(SystemExerciseDefinitions.BACK_SQUAT)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.exerciseDefinitionId");
	}

	private String createOccurrence(AccountId accountId, String planId, String dayId, String scheduledDate)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId
						+ "/occurrences")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"scheduledDate\":\"" + scheduledDate + "\"}"))
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

	private record Workout(
			String occurrenceBase,
			String occurrenceId,
			String executionId,
			String exercisePerformanceKey) {
	}

}
