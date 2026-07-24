package com.devinolabs.uap.athlete.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class AthleteGoalHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullGoalLifecycleAuthCsrfFilteringAndPatchSemantics() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String sportId = createSport(accountId);

		mockMvc.perform(get("/api/v1/athletes/me/goals"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(strengthGoalBody(sportId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult created = mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(strengthGoalBody(sportId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.goalType").value("IMPROVE_STRENGTH"))
				.andExpect(jsonPath("$.title").value("Bench Press"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.priority").value("HIGH"))
				.andExpect(jsonPath("$.targetValue").value(100.0))
				.andExpect(jsonPath("$.athleteSportId").value(sportId))
				.andExpect(jsonPath("$.athleteId").doesNotExist())
				.andExpect(jsonPath("$.accountId").doesNotExist())
				.andExpect(jsonPath("$.normalizedTitle").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist())
				.andReturn();
		String goalId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "goalType":"IMPROVE_ENDURANCE",
								  "title":"10k",
								  "priority":"MEDIUM",
								  "targetDate":"2026-09-01"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/athletes/me/goals").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].title").value("Bench Press"));

		mockMvc.perform(get("/api/v1/athletes/me/goals")
						.param("status", "ACTIVE")
						.param("goalType", "IMPROVE_STRENGTH")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(goalId));

		mockMvc.perform(get("/api/v1/athletes/me/goals/" + goalId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Bench Press"));

		mockMvc.perform(patch("/api/v1/athletes/me/goals/" + goalId)
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Squat PR"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(patch("/api/v1/athletes/me/goals/" + goalId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Squat PR"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Squat PR"))
				.andExpect(jsonPath("$.description").value("Primary lift"))
				.andExpect(jsonPath("$.priority").value("HIGH"))
				.andExpect(jsonPath("$.targetValue").value(100.0))
				.andExpect(jsonPath("$.athleteSportId").value(sportId));

		mockMvc.perform(patch("/api/v1/athletes/me/goals/" + goalId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "description":null,
								  "targetValue":null,
								  "targetUnit":null,
								  "customTargetUnit":null,
								  "targetDate":null,
								  "athleteSportId":null
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.description").value(nullValue()))
				.andExpect(jsonPath("$.targetValue").value(nullValue()))
				.andExpect(jsonPath("$.targetDate").value(nullValue()))
				.andExpect(jsonPath("$.athleteSportId").value(nullValue()))
				.andExpect(jsonPath("$.title").value("Squat PR"));

		mockMvc.perform(patch("/api/v1/athletes/me/goals/" + goalId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"PAUSE"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAUSED"));

		mockMvc.perform(patch("/api/v1/athletes/me/goals/" + goalId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"CANCEL"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(delete("/api/v1/athletes/me/goals/" + goalId)
						.with(accountAuth(accountId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(delete("/api/v1/athletes/me/goals/" + goalId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void validationDuplicateInvalidTransitionMissingProfileAndCrossAccountNotFound() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);

		mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "goalType":"LOSE_WEIGHT",
								  "title":""
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "goalType":"LOSE_WEIGHT",
								  "title":"Cut 5kg",
								  "priority":"MEDIUM"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "goalType":"LOSE_WEIGHT",
								  "title":"cut 5kg",
								  "priority":"HIGH"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ACTIVE_ATHLETE_GOAL"));

		MvcResult created = mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "goalType":"GAIN_MUSCLE",
								  "title":"Lean bulk",
								  "priority":"MEDIUM"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String goalId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(patch("/api/v1/athletes/me/goals/" + goalId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"COMPLETE"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/athletes/me/goals/" + goalId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Cannot edit"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TERMINAL_GOAL_MODIFICATION_REJECTED"));

		mockMvc.perform(patch("/api/v1/athletes/me/goals/" + goalId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"PAUSE"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_GOAL_STATUS_TRANSITION"));

		mockMvc.perform(delete("/api/v1/athletes/me/goals/" + goalId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("GOAL_DELETE_REQUIRES_CANCELLED_STATUS"));

		AccountId missingProfile = AccountId.generate();
		mockMvc.perform(get("/api/v1/athletes/me/goals").with(accountAuth(missingProfile)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_PROFILE_NOT_FOUND"));

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get("/api/v1/athletes/me/goals/" + goalId).with(accountAuth(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_GOAL_NOT_FOUND"));

		String otherSportId = createSport(other);
		mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "goalType":"IMPROVE_SPORT_PERFORMANCE",
								  "title":"Match fitness",
								  "priority":"MEDIUM",
								  "athleteSportId":"%s"
								}
								""".formatted(otherSportId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_SPORT_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "goalType":"IMPROVE_SPORT_PERFORMANCE",
								  "title":"Match fitness",
								  "priority":"MEDIUM",
								  "athleteSportId":"%s"
								}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_SPORT_NOT_FOUND"));
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

	private String createSport(AccountId accountId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/athletes/me/sports")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sportType":"SOCCER",
								  "primarySport":true,
								  "participationLevel":"HIGH_SCHOOL",
								  "yearsExperience":4,
								  "seasonStatus":"IN_SEASON"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static String strengthGoalBody(String sportId) {
		return """
				{
				  "goalType":"IMPROVE_STRENGTH",
				  "title":"Bench Press",
				  "description":"Primary lift",
				  "priority":"HIGH",
				  "targetValue":100.000,
				  "targetUnit":"KILOGRAM",
				  "targetDate":"2026-10-01",
				  "athleteSportId":"%s"
				}
				""".formatted(sportId);
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
