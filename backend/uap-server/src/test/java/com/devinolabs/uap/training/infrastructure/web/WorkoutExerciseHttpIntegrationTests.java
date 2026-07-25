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
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class WorkoutExerciseHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullWorkoutExerciseLifecycleAuthCsrfReorderAndStatus() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises";

		mockMvc.perform(get(base))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"exerciseName":"Back Squat","category":"STRENGTH","type":"BARBELL","sets":3}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult created = mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseName":"Back Squat",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":4,
								  "minimumReps":5,
								  "maximumReps":5,
								  "targetWeight":100,
								  "weightUnit":"KILOGRAM",
								  "targetRestSeconds":120,
								  "targetRpe":8,
								  "tempo":"3-0-1",
								  "coachingNotes":"Brace hard"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.exerciseName").value("Back Squat"))
				.andExpect(jsonPath("$.displayOrder").value(0))
				.andExpect(jsonPath("$.status").value("PLANNED"))
				.andExpect(jsonPath("$.athleteId").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist())
				.andReturn();
		String exerciseId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		MvcResult second = mockMvc.perform(post(base)
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
				.andExpect(status().isCreated())
				.andReturn();
		String exerciseId2 = JsonPath.read(second.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(put(base + "/order")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"exerciseIds":["%s","%s"]}
								""".formatted(exerciseId2, exerciseId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(exerciseId2))
				.andExpect(jsonPath("$[0].displayOrder").value(0))
				.andExpect(jsonPath("$[1].id").value(exerciseId))
				.andExpect(jsonPath("$[1].displayOrder").value(1));

		mockMvc.perform(patch(base + "/" + exerciseId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"coachingNotes":null,"exerciseName":"Back Squat Heavy","sets":5}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.exerciseName").value("Back Squat Heavy"))
				.andExpect(jsonPath("$.sets").value(5))
				.andExpect(jsonPath("$.coachingNotes").value(nullValue()));

		mockMvc.perform(patch(base + "/" + exerciseId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"ACTIVATE"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		mockMvc.perform(delete(base + "/" + exerciseId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_EXERCISE_DELETE_NOT_ALLOWED"));

		mockMvc.perform(delete(base + "/" + exerciseId2)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(base).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].displayOrder").value(0));
	}

	@Test
	void validationErrorsDuplicatesArchivedPlanAndCrossAccount() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises";

		mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"exerciseName":"Missing fields","category":"STRENGTH"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		MvcResult created = mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseName":"Back Squat",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":3,
								  "minimumReps":5,
								  "maximumReps":5
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String exerciseId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseName":"back squat",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":3
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_WORKOUT_EXERCISE"));

		mockMvc.perform(put(base + "/order")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"exerciseIds":["%s"]}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_EXERCISE_ORDER"));

		mockMvc.perform(patch("/api/v1/training/plans/" + planId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"ARCHIVE"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post(base)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseName":"New Lift",
								  "category":"STRENGTH",
								  "type":"DUMBBELL",
								  "sets":2
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_ARCHIVED"));

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get(base + "/" + exerciseId).with(accountAuth(other)))
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
								{"title":"Lower Body","scheduledDay":"MONDAY"}
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
