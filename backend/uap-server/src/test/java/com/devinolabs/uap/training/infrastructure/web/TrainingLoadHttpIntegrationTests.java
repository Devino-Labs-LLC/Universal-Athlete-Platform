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
class TrainingLoadHttpIntegrationTests {

	private static final String HISTORY = "/api/v1/training/training-load/history";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void sessionEffortAndOccurrenceLoadEndpointsRequireAuthentication() throws Exception {
		AccountId accountId = AccountId.generate();
		CompletedWorkout workout = completedWorkout(accountId, "2026-07-25");

		mockMvc.perform(get(workout.occurrenceBase() + "/session-effort"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(get(workout.occurrenceBase() + "/training-load"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void submitUpdateGetSessionEffortAndLoadSummary() throws Exception {
		AccountId accountId = AccountId.generate();
		CompletedWorkout workout = completedWorkout(accountId, "2026-07-25");
		String sessionEffort = workout.occurrenceBase() + "/session-effort";

		mockMvc.perform(post(sessionEffort)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionRpe":8.0,
								  "sessionDurationMinutes":60,
								  "perceivedNotes":"Solid session"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sessionRpe").value(8.0))
				.andExpect(jsonPath("$.sessionDurationMinutes").value(60))
				.andExpect(jsonPath("$.durationSource").value("ATHLETE_REPORTED"))
				.andExpect(jsonPath("$.effortSource").value("ATHLETE_REPORTED"));

		mockMvc.perform(get(sessionEffort).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionRpe").value(8.0));

		mockMvc.perform(get(workout.occurrenceBase() + "/training-load").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completedExerciseCount").value(1))
				.andExpect(jsonPath("$.sessionRpe").value(8.0))
				.andExpect(jsonPath("$.sessionRpeLoad").value(480.00))
				.andExpect(jsonPath("$.sessionRpeLoadUnit").value("ARBITRARY_UNITS"))
				.andExpect(jsonPath("$.totalVolumeUnit").value("KILOGRAM_REPETITIONS"))
				.andExpect(jsonPath("$.calculationVersion").value("V1"));

		mockMvc.perform(patch(sessionEffort)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sessionRpe":8.5,
								  "sessionDurationMinutes":65,
								  "perceivedNotes":"Updated"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sessionRpe").value(8.5))
				.andExpect(jsonPath("$.sessionDurationMinutes").value(65));

		mockMvc.perform(get(sessionEffort + "/revisions").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.revisions", hasSize(1)))
				.andExpect(jsonPath("$.revisions[0].priorSessionRpe").value(8.0))
				.andExpect(jsonPath("$.revisions[0].newSessionRpe").value(8.5));
	}

	@Test
	void duplicateSubmitInvalidRpeAndIncompleteOccurrenceAreRejected() throws Exception {
		AccountId accountId = AccountId.generate();
		CompletedWorkout completed = completedWorkout(accountId, "2026-08-01");
		String sessionEffort = completed.occurrenceBase() + "/session-effort";

		mockMvc.perform(post(sessionEffort)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sessionRpe":7.55,"sessionDurationMinutes":60}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_SESSION_RPE"));

		mockMvc.perform(post(sessionEffort)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sessionRpe":8.0,"sessionDurationMinutes":60}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post(sessionEffort)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sessionRpe":7.0,"sessionDurationMinutes":55}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_SESSION_EFFORT_ALREADY_EXISTS"));

		AccountId otherAccount = AccountId.generate();
		CompletedWorkout inProgress = inProgressWorkout(otherAccount, "2026-08-02");
		mockMvc.perform(post(inProgress.occurrenceBase() + "/session-effort")
						.with(accountAuth(otherAccount))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sessionRpe":7.0,"sessionDurationMinutes":45}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_SESSION_EFFORT_NOT_ALLOWED"));
	}

	@Test
	void recomputeRebuildAndHistoryEndpointsExposeAggregates() throws Exception {
		AccountId accountId = AccountId.generate();
		CompletedWorkout workout = completedWorkout(accountId, "2026-07-27");

		mockMvc.perform(post(workout.occurrenceBase() + "/session-effort")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sessionRpe":7.5,"sessionDurationMinutes":50}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post(workout.occurrenceBase() + "/training-load/recompute")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completedExerciseCount").value(1))
				.andExpect(jsonPath("$.sessionRpeLoad").value(375.00));

		mockMvc.perform(post("/api/v1/training/training-load/rebuild")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completedOccurrencesScanned").value(1))
				.andExpect(jsonPath("$.summariesCreated").value(0))
				.andExpect(jsonPath("$.summariesUpdated").value(0))
				.andExpect(jsonPath("$.summariesUnchanged").value(1));

		mockMvc.perform(get(HISTORY)
						.param("startDate", "2026-07-27")
						.param("endDate", "2026-07-27")
						.param("granularity", "DAILY")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.granularity").value("DAILY"))
				.andExpect(jsonPath("$.dailySummaries", hasSize(1)))
				.andExpect(jsonPath("$.dailySummaries[0].ratedOccurrenceCount").value(1))
				.andExpect(jsonPath("$.dailySummaries[0].averageSessionRpe").value(7.5));

		mockMvc.perform(get(HISTORY)
						.param("startDate", "2026-07-27")
						.param("endDate", "2026-07-27")
						.param("granularity", "OCCURRENCE")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.occurrences", hasSize(1)))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void invalidHistoryRangeIsRejected() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);

		mockMvc.perform(get(HISTORY)
						.param("startDate", "2026-07-30")
						.param("endDate", "2026-07-01")
						.param("granularity", "DAILY")
						.with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAINING_LOAD_DATE_RANGE"));
	}

	private CompletedWorkout completedWorkout(AccountId accountId, String scheduledDate) throws Exception {
		createProfile(accountId);
		return completeWorkout(accountId, scheduledDate);
	}

	private CompletedWorkout inProgressWorkout(AccountId accountId, String scheduledDate) throws Exception {
		createProfile(accountId);
		String planId = planId(accountId, scheduledDate);
		String dayId = dayId(accountId, planId);
		createExercise(accountId, planId, dayId);
		String occurrenceId = createOccurrence(accountId, planId, dayId, scheduledDate);
		String occurrenceBase = occurrenceBase(planId, dayId, occurrenceId);
		return new CompletedWorkout(occurrenceBase, occurrenceId);
	}

	private CompletedWorkout completeWorkout(AccountId accountId, String scheduledDate) throws Exception {
		String planId = planId(accountId, scheduledDate);
		String dayId = dayId(accountId, planId);
		createExercise(accountId, planId, dayId);
		String occurrenceId = createOccurrence(accountId, planId, dayId, scheduledDate);
		String occurrenceBase = occurrenceBase(planId, dayId, occurrenceId);
		String executionId = firstExecutionId(accountId, occurrenceBase);
		String setsBase = occurrenceBase + "/exercises/" + executionId + "/sets";
		List<String> setIds = JsonPath.read(
				mockMvc.perform(get(setsBase).with(accountAuth(accountId)))
						.andExpect(status().isOk())
						.andReturn().getResponse().getContentAsString(),
				"$[*].id");
		logSet(accountId, setsBase, setIds.get(0), 5, "100");
		logSet(accountId, setsBase, setIds.get(1), 5, "100");
		mockMvc.perform(post(occurrenceBase + "/exercises/" + executionId + "/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk());
		mockMvc.perform(post(occurrenceBase + "/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk());
		return new CompletedWorkout(occurrenceBase, occurrenceId);
	}

	private static String occurrenceBase(String planId, String dayId, String occurrenceId) {
		return "/api/v1/training/plans/" + planId + "/days/" + dayId + "/occurrences/" + occurrenceId;
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
				.andExpect(status().isOk());
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
								  "name":"Training Load Plan %s",
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
								  "sets":2,
								  "minimumReps":4,
								  "maximumReps":6
								}
								""".formatted(SystemExerciseDefinitions.BACK_SQUAT)))
				.andExpect(status().isCreated());
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

	private record CompletedWorkout(String occurrenceBase, String occurrenceId) {
	}

}
