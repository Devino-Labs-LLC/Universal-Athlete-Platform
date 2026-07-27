package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class WorkoutExerciseSetHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void setListingAddReorderAndDeleteRespectAuthCsrfAndGuards() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		createExercise(accountId, planId, dayId);
		String occurrenceId = createOccurrence(accountId, planId, dayId, "2026-07-28");
		String executionId = firstExecutionId(accountId, planId, dayId, occurrenceId);
		String setsBase = setsBase(planId, dayId, occurrenceId, executionId);

		mockMvc.perform(get(setsBase))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(get(setsBase).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].setNumber").value(1))
				.andExpect(jsonPath("$[0].displayOrder").value(0))
				.andExpect(jsonPath("$[0].setType").value("WORKING"))
				.andExpect(jsonPath("$[0].status").value("NOT_STARTED"))
				.andExpect(jsonPath("$[0].prescribedMinimumReps").value(8))
				.andExpect(jsonPath("$[0].prescribedMaximumReps").value(10))
				.andExpect(jsonPath("$[2].setNumber").value(3));

		List<String> setIds = listSetIds(accountId, setsBase);
		mockMvc.perform(get(setsBase + "/" + setIds.getFirst()).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(setIds.getFirst()))
				.andExpect(jsonPath("$.workoutExerciseExecutionId").value(executionId));

		mockMvc.perform(get(setsBase + "/" + UUID.randomUUID()).with(accountAuth(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKOUT_EXERCISE_SET_NOT_FOUND"));

		mockMvc.perform(post(setsBase).with(accountAuth(accountId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		String addedSetId = JsonPath.read(mockMvc.perform(post(setsBase)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"setType":"BACK_OFF","prescribedMinimumReps":5,"prescribedMaximumReps":5}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.setNumber").value(4))
				.andExpect(jsonPath("$.displayOrder").value(3))
				.andExpect(jsonPath("$.setType").value("BACK_OFF"))
				.andExpect(jsonPath("$.prescribedMinimumReps").value(5))
				.andExpect(jsonPath("$.status").value("NOT_STARTED"))
				.andReturn().getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(setsBase + "/reorder")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"setIds\":[\"" + addedSetId + "\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_EXERCISE_SET_MEMBERSHIP"));

		mockMvc.perform(post(setsBase + "/reorder")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"setIds\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		List<String> reordered = List.of(addedSetId, setIds.get(2), setIds.get(1), setIds.get(0));
		mockMvc.perform(post(setsBase + "/reorder")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(setIdsBody(reordered)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4)))
				.andExpect(jsonPath("$[0].id").value(addedSetId))
				.andExpect(jsonPath("$[0].setNumber").value(1))
				.andExpect(jsonPath("$[3].id").value(setIds.getFirst()))
				.andExpect(jsonPath("$[3].setNumber").value(4));

		mockMvc.perform(delete(setsBase + "/" + addedSetId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(setsBase).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].id").value(setIds.get(2)))
				.andExpect(jsonPath("$[0].setNumber").value(1))
				.andExpect(jsonPath("$[2].setNumber").value(3));

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get(setsBase).with(accountAuth(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));
	}

	@Test
	void loggingSetsPromotesParentsAndBlocksTerminalMutations() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		createExercise(accountId, planId, dayId);
		String occurrenceId = createOccurrence(accountId, planId, dayId, "2026-07-29");
		String executionId = firstExecutionId(accountId, planId, dayId, occurrenceId);
		String occurrenceBase = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences/" + occurrenceId;
		String setsBase = setsBase(planId, dayId, occurrenceId, executionId);
		List<String> setIds = listSetIds(accountId, setsBase);

		mockMvc.perform(patch(setsBase + "/" + setIds.getFirst())
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"actualRpe":11}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		mockMvc.perform(patch(setsBase + "/" + setIds.getFirst())
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"actualReps":9,"actualWeight":100.00,"actualWeightUnit":"KILOGRAM","actualRestSeconds":120,
								 "actualRpe":8.50,"athleteNotes":"Felt strong"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.startedAt").isNotEmpty())
				.andExpect(jsonPath("$.actualReps").value(9))
				.andExpect(jsonPath("$.actualWeightUnit").value("KILOGRAM"))
				.andExpect(jsonPath("$.athleteNotes").value("Felt strong"));

		mockMvc.perform(get(occurrenceBase).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.executions[0].status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.executions[0].setCount").value(3))
				.andExpect(jsonPath("$.executions[0].inProgressSetCount").value(1))
				.andExpect(jsonPath("$.executions[0].notStartedSetCount").value(2));

		mockMvc.perform(post(setsBase + "/" + setIds.getFirst() + "/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.completedAt").isNotEmpty());

		mockMvc.perform(patch(setsBase + "/" + setIds.getFirst())
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"actualReps":10}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_EXERCISE_SET_STATUS"));

		mockMvc.perform(delete(setsBase + "/" + setIds.getFirst())
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_EXERCISE_SET_DELETE_NOT_ALLOWED"));

		mockMvc.perform(post(setsBase + "/reorder")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(setIdsBody(setIds)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_EXERCISE_SET_REORDER_NOT_ALLOWED"));

		mockMvc.perform(post(setsBase + "/" + setIds.get(1) + "/start")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		mockMvc.perform(post(setsBase + "/" + setIds.get(1) + "/skip")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SKIPPED"));

		mockMvc.perform(post(setsBase + "/" + setIds.get(2) + "/skip")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SKIPPED"));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences/"
						+ occurrenceId + "/exercises/" + executionId + "/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.actualSets").value(1))
				.andExpect(jsonPath("$.actualReps").value(9))
				.andExpect(jsonPath("$.actualWeight").value(100.00))
				.andExpect(jsonPath("$.weightUnit").value("KILOGRAM"))
				.andExpect(jsonPath("$.actualRestSeconds").value(120))
				.andExpect(jsonPath("$.actualRpe").value(8.50))
				.andExpect(jsonPath("$.completedSetCount").value(1))
				.andExpect(jsonPath("$.skippedSetCount").value(2));

		mockMvc.perform(post(setsBase)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_EXERCISE_EXECUTION_STATUS"));
	}

	@Test
	void skippingAnOccurrenceSkipsEveryActiveSet() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		createExercise(accountId, planId, dayId);
		String occurrenceId = createOccurrence(accountId, planId, dayId, "2026-07-30");
		String executionId = firstExecutionId(accountId, planId, dayId, occurrenceId);
		String setsBase = setsBase(planId, dayId, occurrenceId, executionId);

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId
						+ "/occurrences/" + occurrenceId + "/skip")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SKIPPED"));

		mockMvc.perform(get(setsBase).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].status").value("SKIPPED"))
				.andExpect(jsonPath("$[1].status").value("SKIPPED"))
				.andExpect(jsonPath("$[2].status").value("SKIPPED"));

		List<String> setIds = listSetIds(accountId, setsBase);
		mockMvc.perform(post(setsBase + "/" + setIds.getFirst() + "/start")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_OCCURRENCE_STATUS"));
	}

	private String setsBase(String planId, String dayId, String occurrenceId, String executionId) {
		return "/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences/" + occurrenceId
				+ "/exercises/" + executionId + "/sets";
	}

	private String firstExecutionId(AccountId accountId, String planId, String dayId, String occurrenceId)
			throws Exception {
		String body = mockMvc.perform(get("/api/v1/training/plans/" + planId + "/days/" + dayId
						+ "/occurrences/" + occurrenceId + "/exercises").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$[0].id");
	}

	private List<String> listSetIds(AccountId accountId, String setsBase) throws Exception {
		String body = mockMvc.perform(get(setsBase).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$[*].id");
	}

	private static String setIdsBody(List<String> setIds) {
		return "{\"setIds\":[" + setIds.stream().map(id -> "\"" + id + "\"").reduce((a, b) -> a + "," + b).orElse("")
				+ "]}";
	}

	private void createProfile(AccountId accountId) throws Exception {
		mockMvc.perform(post("/api/v1/athletes/me")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName":"Riley",
								  "lastName":"Chen",
								  "dateOfBirth":"1997-03-04",
								  "sex":"MALE",
								  "heightCm":182.00,
								  "weightKg":84.00,
								  "dominantHand":"RIGHT",
								  "dominantFoot":"LEFT"
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
								  "name":"Set Logging Plan",
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
								{"title":"Squat Day","planWeekNumber":1,"scheduledDayOfWeek":"TUESDAY"}
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
								  "exerciseDefinitionId":"%s",
								  "exerciseName":"Back Squat",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":3,
								  "minimumReps":8,
								  "maximumReps":10,
								  "targetRestSeconds":120
								}
								""".formatted(SystemExerciseDefinitions.BACK_SQUAT)))
				.andExpect(status().isCreated());
	}

	private String createOccurrence(AccountId accountId, String planId, String dayId, String scheduledDate)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences")
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

}
