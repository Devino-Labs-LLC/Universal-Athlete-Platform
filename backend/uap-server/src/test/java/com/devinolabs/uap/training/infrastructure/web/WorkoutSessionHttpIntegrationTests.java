package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class WorkoutSessionHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullSessionLifecycleAuthCsrfUpdateCompleteAndSkip() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		String exerciseId = createExercise(accountId, planId, dayId, "Back Squat");
		String otherExerciseId = createExercise(accountId, planId, dayId, "Romanian Deadlift");
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises/" + exerciseId;

		mockMvc.perform(get(base + "/session"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post(base + "/start").with(accountAuth(accountId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(get(base + "/session").with(accountAuth(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKOUT_SESSION_NOT_FOUND"));

		mockMvc.perform(post(base + "/start").with(accountAuth(accountId)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$.completedAt").value(nullValue()))
				.andExpect(jsonPath("$.athleteId").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist());

		mockMvc.perform(patch(base + "/session")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "actualSets":4,
								  "actualReps":5,
								  "actualWeight":102.5,
								  "weightUnit":"KILOGRAM",
								  "actualRestSeconds":90,
								  "actualRpe":8,
								  "athleteNotes":"Felt strong"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.actualSets").value(4))
				.andExpect(jsonPath("$.actualWeight").value(102.5))
				.andExpect(jsonPath("$.athleteNotes").value("Felt strong"));

		mockMvc.perform(post(base + "/complete").with(accountAuth(accountId)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.completedAt").isNotEmpty());

		mockMvc.perform(get(base + "/session").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));

		String otherBase = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises/" + otherExerciseId;
		mockMvc.perform(post(otherBase + "/skip").with(accountAuth(accountId)).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SKIPPED"))
				.andExpect(jsonPath("$.completedAt").value(nullValue()));
	}

	@Test
	void validationArchivedPlanAndCrossAccount() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);
		String exerciseId = createExercise(accountId, planId, dayId, "Back Squat");
		String base = "/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises/" + exerciseId;

		mockMvc.perform(post(base + "/start").with(accountAuth(accountId)).with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(patch(base + "/session")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"actualWeight":10}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		mockMvc.perform(patch("/api/v1/training/plans/" + planId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"ARCHIVE"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post(base + "/complete").with(accountAuth(accountId)).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_ARCHIVED"));

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get(base + "/session").with(accountAuth(other)))
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

	private String createExercise(AccountId accountId, String planId, String dayId, String name) throws Exception {
		MvcResult result = mockMvc.perform(post(
						"/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseName":"%s",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":3,
								  "minimumReps":5,
								  "maximumReps":5
								}
								""".formatted(name)))
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
